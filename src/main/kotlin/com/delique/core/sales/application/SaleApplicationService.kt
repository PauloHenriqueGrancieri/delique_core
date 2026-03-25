package com.delique.core.sales.application

import com.delique.core.inventory.application.StockManagementService
import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.port.ComboRepository
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.marketing.infrastructure.persistence.MarketingCampaignJpa
import com.delique.core.pricing.infrastructure.persistence.CreditCardInstallmentFeeJpa
import com.delique.core.pricing.infrastructure.persistence.PaymentMethodConfigJpa
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.product.domain.port.ProductVariationOptionRepository
import com.delique.core.sales.application.dto.MultipleSalesDto
import com.delique.core.sales.application.dto.OrderDto
import com.delique.core.sales.application.dto.SaleLineDto
import com.delique.core.sales.domain.model.CustomerOrder
import com.delique.core.sales.domain.model.SaleLine
import com.delique.core.sales.infrastructure.persistence.ComboItemJpa
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import com.delique.core.sales.infrastructure.persistence.SaleLineJpa
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class SaleApplicationService(
    private val saleLineJpa: SaleLineJpa,
    private val customerOrderJpa: CustomerOrderJpa,
    private val productRepository: ProductRepository,
    private val productVariationOptionRepository: ProductVariationOptionRepository,
    private val clientApplicationService: ClientApplicationService,
    private val stockMovementRepository: StockMovementRepository,
    @Lazy private val stockManagementService: StockManagementService,
    @Lazy private val comboRepository: ComboRepository,
    private val comboItemJpa: ComboItemJpa,
    private val paymentMethodConfigJpa: PaymentMethodConfigJpa,
    private val creditCardInstallmentFeeJpa: CreditCardInstallmentFeeJpa,
    private val marketingCampaignJpa: MarketingCampaignJpa,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun getFeePercentage(paymentMethod: String, installments: Int): BigDecimal? =
        try {
            val method = paymentMethod.trim().uppercase()
            if (method == "CREDIT_CARD") {
                val n = installments.coerceIn(1, 12)
                val fee = creditCardInstallmentFeeJpa.findByInstallments(n) ?: return null
                if (fee.feePercentage != null && fee.feePercentage!! > BigDecimal.ZERO) fee.feePercentage else null
            } else {
                val config = paymentMethodConfigJpa.findByPaymentMethod(method) ?: return null
                if (config.feePercentage != null && config.feePercentage!! > BigDecimal.ZERO) config.feePercentage else null
            }
        } catch (_: Exception) {
            null
        }

    fun getAllOrders(): List<OrderDto> =
        customerOrderJpa.findAllWithSales().map { it.toDto() }

    fun getOrderById(id: Long): OrderDto {
        val order = customerOrderJpa.findByIdWithSales(id)
            ?: run {
                log.warn("Order not found: id=$id")
                throw IllegalArgumentException("Order not found with id: $id")
            }
        return order.toDto()
    }

    @Transactional
    fun createOrderWithSales(
        items: List<SaleLineDto>,
        paymentMethod: String,
        clientId: Long?,
        orderDiscountValue: BigDecimal? = null,
        orderDiscountPercent: BigDecimal? = null,
        installments: Int? = null,
        campaignId: Long? = null,
    ): OrderDto {
        val client = clientId?.let { clientApplicationService.getClientEntity(it) }
        val campaign = campaignId?.let { marketingCampaignJpa.findById(it).orElse(null) }

        val hasValue = orderDiscountValue != null && orderDiscountValue > BigDecimal.ZERO
        val hasPercent = orderDiscountPercent != null && orderDiscountPercent > BigDecimal.ZERO
        if (hasValue && hasPercent) {
            throw IllegalArgumentException("Provide either orderDiscountValue or orderDiscountPercent, not both")
        }

        for (item in items) {
            val product = productRepository.findById(item.productId)
                ?: throw IllegalArgumentException("Product not found: ${item.productId}")
            val hasVariations = productVariationOptionRepository.findByProductId(product.id).isNotEmpty()
            if (hasVariations && item.variationOptionId == null) {
                throw IllegalArgumentException("Product '${product.name}' requires variationOptionId on the sale line.")
            }
            val combo = comboRepository.findByProductId(item.productId)
            if (combo != null) {
                val comboItems = comboItemJpa.findByCombo_Id(combo.id)
                for (ci in comboItems) {
                    val required = item.quantity * ci.quantity
                    val currentStock = stockManagementService.getCurrentStock(ci.product.id)
                    if (currentStock < required) {
                        throw IllegalArgumentException(
                            "Insufficient stock for combo '${combo.name}': product '${ci.product.name}' needs $required, available: $currentStock",
                        )
                    }
                }
            } else {
                val currentStock = stockManagementService.getCurrentStock(item.productId, item.variationOptionId)
                if (currentStock < item.quantity) {
                    throw IllegalArgumentException(
                        "Insufficient stock for product ${item.productId}. Available: $currentStock, Requested: ${item.quantity}",
                    )
                }
            }
        }

        val subtotal = items.sumOf {
            it.unitPrice.multiply(BigDecimal(it.quantity)).subtract(it.discount ?: BigDecimal.ZERO)
        }
        val discountAmount = when {
            hasPercent -> subtotal.multiply(orderDiscountPercent!!).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            hasValue -> orderDiscountValue!!
            else -> BigDecimal.ZERO
        }
        val totalWithDiscount = subtotal.subtract(discountAmount)
        val method = paymentMethod.trim().ifBlank { "MONEY" }
        val feePct = getFeePercentage(method, installments ?: 1)
        val feeValue = if (feePct != null && feePct > BigDecimal.ZERO) {
            totalWithDiscount.multiply(feePct).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        } else {
            null
        }

        val order = CustomerOrder(
            paymentMethod = method,
            client = client,
            orderDiscountValue = if (discountAmount > BigDecimal.ZERO) discountAmount else null,
            feePercentage = feePct,
            feeValue = feeValue,
            campaign = campaign,
        )
        val savedOrder = customerOrderJpa.save(order)
        val savedSales = mutableListOf<SaleLine>()

        for (item in items) {
            val product = productRepository.findById(item.productId)!!
            val combo = comboRepository.findByProductId(item.productId)
            if (combo != null) {
                val comboItems = comboItemJpa.findByCombo_Id(combo.id)
                for (ci in comboItems) {
                    val qty = item.quantity * ci.quantity
                    stockManagementService.consumeStockFifo(ci.product.id, qty)
                }
                val sale = SaleLine(
                    product = product,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    customerOrder = savedOrder,
                )
                val savedSale = saleLineJpa.save(sale)
                savedSales.add(savedSale)
                for (ci in comboItems) {
                    val qty = item.quantity * ci.quantity
                    stockMovementRepository.save(
                        StockMovement(
                            product = ci.product,
                            quantity = qty,
                            type = MovementType.SALE,
                            details = "#${savedOrder.id}",
                            saleId = savedSale.id,
                        ),
                    )
                }
            } else {
                stockManagementService.consumeStockFifo(item.productId, item.quantity, item.variationOptionId)
                val variationOption = item.variationOptionId?.let { optId ->
                    productVariationOptionRepository.findByProductIdAndId(product.id, optId)
                }
                val sale = SaleLine(
                    product = product,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    customerOrder = savedOrder,
                    variationOption = variationOption,
                )
                val savedSale = saleLineJpa.save(sale)
                savedSales.add(savedSale)
                stockMovementRepository.save(
                    StockMovement(
                        product = product,
                        quantity = item.quantity,
                        type = MovementType.SALE,
                        details = "#${savedOrder.id}",
                        saleId = savedSale.id,
                        variationOption = variationOption,
                    ),
                )
            }
        }

        savedOrder.sales.clear()
        savedOrder.sales.addAll(savedSales)
        customerOrderJpa.save(savedOrder)
        log.info("Sale completed. Order #${savedOrder.id}, total with discount: $totalWithDiscount")
        return savedOrder.toDto()
    }

    @Transactional
    fun updateOrder(
        orderId: Long,
        items: List<SaleLineDto>,
        paymentMethod: String,
        clientId: Long?,
        orderDiscountValue: BigDecimal? = null,
        orderDiscountPercent: BigDecimal? = null,
        installments: Int? = null,
        createdAt: java.time.LocalDateTime? = null,
        campaignId: Long? = null,
    ): OrderDto {
        val order = customerOrderJpa.findByIdWithSales(orderId)
            ?: throw IllegalArgumentException("Order not found with id: $orderId")

        val client = clientId?.let { clientApplicationService.getClientEntity(it) }
        val campaign = campaignId?.let { marketingCampaignJpa.findById(it).orElse(null) }

        val hasValue = orderDiscountValue != null && orderDiscountValue > BigDecimal.ZERO
        val hasPercent = orderDiscountPercent != null && orderDiscountPercent > BigDecimal.ZERO
        if (hasValue && hasPercent) {
            throw IllegalArgumentException("Provide either orderDiscountValue or orderDiscountPercent, not both")
        }

        for (sale in order.sales) {
            stockManagementService.restoreStockFromSale(sale.id)
        }
        saleLineJpa.deleteAll(order.sales)
        order.sales.clear()

        for (item in items) {
            val product = productRepository.findById(item.productId)
                ?: throw IllegalArgumentException("Product not found: ${item.productId}")
            val hasVariations = productVariationOptionRepository.findByProductId(product.id).isNotEmpty()
            if (hasVariations && item.variationOptionId == null) {
                throw IllegalArgumentException("Product '${product.name}' requires variationOptionId on the sale line.")
            }
            val combo = comboRepository.findByProductId(item.productId)
            if (combo != null) {
                val comboItems = comboItemJpa.findByCombo_Id(combo.id)
                for (ci in comboItems) {
                    val required = item.quantity * ci.quantity
                    val currentStock = stockManagementService.getCurrentStock(ci.product.id)
                    if (currentStock < required) {
                        throw IllegalArgumentException(
                            "Insufficient stock for combo '${combo.name}': product '${ci.product.name}' needs $required, available: $currentStock",
                        )
                    }
                }
            } else {
                val currentStock = stockManagementService.getCurrentStock(item.productId, item.variationOptionId)
                if (currentStock < item.quantity) {
                    throw IllegalArgumentException(
                        "Insufficient stock for product ${item.productId}. Available: $currentStock, Requested: ${item.quantity}",
                    )
                }
            }
        }

        val subtotal = items.sumOf {
            it.unitPrice.multiply(BigDecimal(it.quantity)).subtract(it.discount ?: BigDecimal.ZERO)
        }
        val discountAmount = when {
            hasPercent -> subtotal.multiply(orderDiscountPercent!!).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            hasValue -> orderDiscountValue!!
            else -> BigDecimal.ZERO
        }
        val totalWithDiscount = subtotal.subtract(discountAmount)
        val method = paymentMethod.trim().ifBlank { "MONEY" }
        val feePct = getFeePercentage(method, installments ?: 1)
        val feeValue = if (feePct != null && feePct > BigDecimal.ZERO) {
            totalWithDiscount.multiply(feePct).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        } else {
            null
        }

        order.paymentMethod = method
        order.client = client
        order.orderDiscountValue = if (discountAmount > BigDecimal.ZERO) discountAmount else null
        order.feePercentage = feePct
        order.feeValue = feeValue
        order.campaign = campaign
        if (createdAt != null) order.createdAt = createdAt

        val savedSales = mutableListOf<SaleLine>()
        for (item in items) {
            val product = productRepository.findById(item.productId)!!
            val combo = comboRepository.findByProductId(item.productId)
            if (combo != null) {
                val comboItems = comboItemJpa.findByCombo_Id(combo.id)
                for (ci in comboItems) {
                    val qty = item.quantity * ci.quantity
                    stockManagementService.consumeStockFifo(ci.product.id, qty)
                }
                val sale = SaleLine(
                    product = product,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    customerOrder = order,
                )
                val savedSale = saleLineJpa.save(sale)
                savedSales.add(savedSale)
                for (ci in comboItems) {
                    val qty = item.quantity * ci.quantity
                    stockMovementRepository.save(
                        StockMovement(
                            product = ci.product,
                            quantity = qty,
                            type = MovementType.SALE,
                            details = "#${order.id}",
                            saleId = savedSale.id,
                        ),
                    )
                }
            } else {
                stockManagementService.consumeStockFifo(item.productId, item.quantity, item.variationOptionId)
                val variationOption = item.variationOptionId?.let { optId ->
                    productVariationOptionRepository.findByProductIdAndId(product.id, optId)
                }
                val sale = SaleLine(
                    product = product,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    customerOrder = order,
                    variationOption = variationOption,
                )
                val savedSale = saleLineJpa.save(sale)
                savedSales.add(savedSale)
                stockMovementRepository.save(
                    StockMovement(
                        product = product,
                        quantity = item.quantity,
                        type = MovementType.SALE,
                        details = "#${order.id}",
                        saleId = savedSale.id,
                        variationOption = variationOption,
                    ),
                )
            }
        }

        order.sales.clear()
        order.sales.addAll(savedSales)
        customerOrderJpa.save(order)
        log.info("Sale updated. Order #${order.id}, total with discount: $totalWithDiscount")
        return order.toDto()
    }

    private fun CustomerOrder.toDto() = OrderDto(
        id = id,
        paymentMethod = paymentMethod,
        clientId = client?.id,
        createdAt = createdAt,
        orderDiscountValue = orderDiscountValue,
        feePercentage = feePercentage,
        feeValue = feeValue,
        campaignId = campaign?.id,
        campaignName = campaign?.name,
        items = sales.map { it.toDto() },
    )

    private fun SaleLine.toDto() = SaleLineDto(
        id = id,
        productId = product.id,
        productName = product.name,
        productDescription = product.description,
        quantity = quantity,
        unitPrice = unitPrice,
        discount = discount,
        orderId = customerOrder.id,
        createdAt = createdAt,
        variationOptionId = variationOption?.id,
        variationOptionName = variationOption?.name,
        brandId = product.brand.id,
        brandName = product.brand.name,
    )
}
