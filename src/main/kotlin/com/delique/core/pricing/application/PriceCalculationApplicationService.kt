package com.delique.core.pricing.application

import com.delique.core.analytics.domain.model.PeriodType
import com.delique.core.analytics.infrastructure.persistence.ProductClassificationJpa
import com.delique.core.catalog.domain.model.Catalog
import com.delique.core.catalog.infrastructure.persistence.CatalogJpa
import com.delique.core.inventory.application.StockManagementService
import com.delique.core.pricing.application.dto.*
import com.delique.core.pricing.domain.model.PendingPriceCalculation
import com.delique.core.pricing.domain.model.PriceCalculationConfig
import com.delique.core.pricing.domain.pipeline.PricingContext
import com.delique.core.pricing.domain.pipeline.PricingStrategy
import com.delique.core.pricing.infrastructure.persistence.MarginStrategyJpa
import com.delique.core.pricing.infrastructure.persistence.PendingPriceCalculationJpa
import com.delique.core.pricing.infrastructure.persistence.PriceCalculationConfigJpa
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import com.delique.core.sales.infrastructure.persistence.SaleLineJpa
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class PriceCalculationApplicationService(
    private val pricingStrategy: PricingStrategy,
    private val priceCalculationConfigJpa: PriceCalculationConfigJpa,
    private val customerOrderJpa: CustomerOrderJpa,
    private val saleLineJpa: SaleLineJpa,
    private val productRepository: ProductRepository,
    @Lazy private val stockManagementService: StockManagementService,
    private val pendingPriceCalculationJpa: PendingPriceCalculationJpa,
    private val catalogJpa: CatalogJpa,
    private val marginStrategyJpa: MarginStrategyJpa,
    private val productClassificationJpa: ProductClassificationJpa,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getDefaultConfig(): PriceCalculationConfig {
        val config = priceCalculationConfigJpa.findFirstByOrderByIdAsc() ?: PriceCalculationConfig()
        config.defaultAverageItemsPerOrder = calculateAverageItemsPerOrder()
        return config
    }

    fun calculateAverageItemsPerOrder(): BigDecimal {
        val totalOrders = customerOrderJpa.count()
        if (totalOrders == 0L) return BigDecimal.ONE
        val totalItems = saleLineJpa.count()
        if (totalItems == 0L) return BigDecimal.ONE
        return BigDecimal(totalItems).divide(BigDecimal(totalOrders), 2, RoundingMode.HALF_UP)
    }

    fun getPurchaseCostFromStock(productId: Long): BigDecimal =
        try {
            stockManagementService.getStockSummary(productId).averageCost
        } catch (_: Exception) {
            BigDecimal.ZERO
        }

    fun calculatePrice(request: PriceCalculationRequest): PriceCalculationResponse {
        val cmv = request.cmv ?: throw IllegalArgumentException("CMV é obrigatório")
        val cfg = getDefaultConfig()
        val loss = request.lossPercentage ?: cfg.defaultLossPercentage
        val ctxIn = PricingContext(
            cmv = cmv,
            lossPercentage = loss,
            salesCommissionPercentage = request.salesCommissionPercentage ?: cfg.defaultSalesCommissionPercentage,
            cardFeePercentage = request.cardFeePercentage ?: cfg.defaultCardFeePercentage,
            taxPercentage = request.taxPercentage ?: cfg.defaultTaxPercentage,
            packagingValue = request.packagingValue ?: cfg.defaultPackagingValue,
            deliveryValue = request.deliveryValue ?: cfg.defaultDeliveryValue,
            averageItemsPerOrder = request.averageItemsPerOrder ?: cfg.defaultAverageItemsPerOrder,
            fixedExpensePercentage = request.fixedExpensePercentage ?: cfg.defaultFixedExpensePercentage,
            profitMarginPercentage = request.profitMarginPercentage ?: cfg.defaultProfitMarginPercentage,
            productId = request.productId,
            productName = "",
            productDescription = null,
        )
        val (pid, pname, pdesc) = if (request.productId != null) {
            val p = productRepository.findById(request.productId!!) ?: throw IllegalArgumentException("Product not found")
            Triple(p.id, p.name, p.description)
        } else {
            Triple(0L, "Valor informado", null)
        }
        val ctx = pricingStrategy.calculate(ctxIn.copy(productName = pname, productDescription = pdesc))
        return PriceCalculationResponse(
            productId = pid,
            productName = pname,
            productDescription = pdesc,
            cmv = ctx.cmvAjustado,
            markup = ctx.markup,
            calculatedPrice = ctx.calculatedPrice,
            finalPrice = null,
            lossPercentage = loss,
            salesCommissionPercentage = ctxIn.salesCommissionPercentage,
            cardFeePercentage = ctxIn.cardFeePercentage,
            taxPercentage = ctxIn.taxPercentage,
            packagingValue = ctxIn.packagingValue,
            deliveryValue = ctxIn.deliveryValue,
            averageItemsPerOrder = ctxIn.averageItemsPerOrder,
            custoPedidoUnitario = ctx.custoPedidoUnitario,
            percentuais = ctx.percentuaisSum,
            fixedExpensePercentage = ctxIn.fixedExpensePercentage,
            profitMarginPercentage = ctxIn.profitMarginPercentage,
        )
    }

    @Transactional
    fun saveToCatalog(response: PriceCalculationResponse): com.delique.core.catalog.application.dto.CatalogDto {
        if (response.productId == 0L) {
            throw IllegalArgumentException("Salvar no catálogo requer um produto selecionado")
        }
        val product = productRepository.findById(response.productId)!!
        val priceToSave = response.finalPrice ?: response.calculatedPrice
        val existing = catalogJpa.findByProduct(product)
        val finalPrice = priceToSave
        val catalog = existing ?: Catalog(
            product = product,
            costPrice = response.cmv,
            salePrice = priceToSave,
            discountPercentage = BigDecimal.ZERO,
            finalPrice = finalPrice,
            inCatalog = true,
        )
        catalog.costPrice = response.cmv
        catalog.salePrice = priceToSave
        catalog.discountPercentage = BigDecimal.ZERO
        catalog.finalPrice = finalPrice
        catalog.inCatalog = true
        val saved = catalogJpa.save(catalog)
        pendingPriceCalculationJpa.findByProduct_Id(product.id)?.let { pendingPriceCalculationJpa.delete(it) }
        return com.delique.core.catalog.application.dto.CatalogDto(
            id = saved.id,
            productId = product.id,
            productName = product.name,
            costPrice = saved.costPrice,
            salePrice = saved.salePrice,
            discountPercentage = saved.discountPercentage,
            finalPrice = saved.finalPrice,
            inCatalog = saved.inCatalog,
        )
    }

    @Transactional
    fun updateDefaultConfig(config: PriceCalculationConfig): PriceCalculationConfig {
        val existing = priceCalculationConfigJpa.findFirstByOrderByIdAsc() ?: PriceCalculationConfig()
        existing.defaultCmv = config.defaultCmv
        existing.defaultLossPercentage = config.defaultLossPercentage
        existing.defaultSalesCommissionPercentage = config.defaultSalesCommissionPercentage
        existing.defaultCardFeePercentage = config.defaultCardFeePercentage
        existing.defaultTaxPercentage = config.defaultTaxPercentage
        existing.defaultPackagingValue = config.defaultPackagingValue
        existing.defaultDeliveryValue = config.defaultDeliveryValue
        existing.defaultAverageItemsPerOrder = config.defaultAverageItemsPerOrder
        existing.defaultFixedExpensePercentage = config.defaultFixedExpensePercentage
        existing.defaultProfitMarginPercentage = config.defaultProfitMarginPercentage
        return priceCalculationConfigJpa.save(existing)
    }

    fun getProductAnalytics(productId: Long): ProductAnalyticsResponse? {
        val product = productRepository.findById(productId) ?: return null
        val sales = saleLineJpa.findByProduct(product)
        if (sales.isEmpty()) {
            return ProductAnalyticsResponse(productId, false, null, null, null, null, null, null)
        }
        val classification = productClassificationJpa.findByProductAndPeriodType(product, PeriodType.LAST_90_DAYS)
        val abcF = classification?.abcFaturamento?.name
        val abcM = classification?.abcMargem?.name
        val xyz = classification?.xyzGiro?.name
        val classificationString = if (abcF != null && abcM != null && xyz != null) "$abcF-$abcM-$xyz" else null
        val strategy = marginStrategyJpa.findBestMatch(
            classification?.abcFaturamento,
            classification?.abcMargem,
            classification?.xyzGiro,
        ).firstOrNull()
        return ProductAnalyticsResponse(
            productId = productId,
            hasSalesHistory = true,
            classification = classificationString,
            abcFaturamento = abcF,
            abcMargem = abcM,
            xyzGiro = xyz,
            suggestedMarginPercentage = strategy?.suggestedMarginPercentage,
            strategyDescription = strategy?.description,
        )
    }

    @Transactional
    fun createPendingPriceCalculation(
        productId: Long,
        currentSalePrice: BigDecimal,
        calculatedPrice: BigDecimal,
        cmv: BigDecimal,
    ): PendingPriceCalculation {
        val product = productRepository.findById(productId)!!
        val existing = pendingPriceCalculationJpa.findByProduct_Id(productId)
        val pending = existing ?: PendingPriceCalculation(
            product = product,
            currentSalePrice = currentSalePrice,
            calculatedPrice = calculatedPrice,
            finalPrice = calculatedPrice,
            cmv = cmv,
        )
        pending.currentSalePrice = currentSalePrice
        pending.calculatedPrice = calculatedPrice
        pending.finalPrice = calculatedPrice
        pending.cmv = cmv
        return pendingPriceCalculationJpa.save(pending)
    }

    fun getAllPendingPriceCalculations(): List<PendingPriceCalculationDto> =
        pendingPriceCalculationJpa.findAllByOrderByCreatedAtDesc().map {
            PendingPriceCalculationDto(
                id = it.id,
                productId = it.product.id,
                productName = it.product.name,
                currentSalePrice = it.currentSalePrice,
                calculatedPrice = it.calculatedPrice,
                finalPrice = it.finalPrice ?: it.calculatedPrice,
                cmv = it.cmv,
                createdAt = it.createdAt,
            )
        }

    @Transactional
    fun approvePendingPriceCalculation(id: Long, finalPrice: BigDecimal?): com.delique.core.catalog.application.dto.CatalogDto {
        val pending = pendingPriceCalculationJpa.findById(id).orElseThrow { IllegalArgumentException("Pending price calculation not found: $id") }
        val priceToSave = finalPrice ?: pending.calculatedPrice
        val product = pending.product
        val catalog = catalogJpa.findByProduct(product) ?: Catalog(
            product = product,
            costPrice = pending.cmv,
            salePrice = priceToSave,
            discountPercentage = BigDecimal.ZERO,
            finalPrice = priceToSave,
            inCatalog = true,
        )
        catalog.costPrice = pending.cmv
        catalog.salePrice = priceToSave
        catalog.finalPrice = priceToSave
        catalog.inCatalog = true
        val saved = catalogJpa.save(catalog)
        pendingPriceCalculationJpa.delete(pending)
        return com.delique.core.catalog.application.dto.CatalogDto(
            id = saved.id,
            productId = product.id,
            productName = product.name,
            costPrice = saved.costPrice,
            salePrice = saved.salePrice,
            discountPercentage = saved.discountPercentage,
            finalPrice = saved.finalPrice,
            inCatalog = saved.inCatalog,
        )
    }

    @Transactional
    fun rejectPendingPriceCalculation(id: Long) {
        val pending = pendingPriceCalculationJpa.findById(id).orElseThrow { IllegalArgumentException("Pending price calculation not found: $id") }
        pendingPriceCalculationJpa.delete(pending)
    }
}
