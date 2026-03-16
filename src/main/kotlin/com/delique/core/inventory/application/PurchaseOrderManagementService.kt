package com.delique.core.inventory.application

import com.delique.core.inventory.application.dto.*
import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.PurchaseOrder
import com.delique.core.inventory.domain.model.PurchaseOrderItem
import com.delique.core.inventory.domain.model.PurchaseOrderStatus
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.model.StockUnit
import com.delique.core.inventory.domain.model.StockUnitExpiry
import com.delique.core.inventory.domain.model.StockUnitExpiryId
import com.delique.core.inventory.domain.port.PurchaseOrderRepository
import com.delique.core.inventory.domain.port.StockEntryPricingPort
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.inventory.domain.port.StockUnitExpiryRepository
import com.delique.core.inventory.domain.port.StockUnitRepository
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.product.domain.port.ProductVariationOptionRepository
import com.delique.core.supplier.domain.port.SupplierRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.YearMonth
import java.util.UUID

@Service
class PurchaseOrderManagementService(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val productRepository: ProductRepository,
    private val variationOptionRepository: ProductVariationOptionRepository,
    private val supplierRepository: SupplierRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockUnitRepository: StockUnitRepository,
    private val stockUnitExpiryRepository: StockUnitExpiryRepository,
    private val stockManagementService: StockManagementService,
    private val stockEntryPricingPort: StockEntryPricingPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun generateStockUnitId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString()
        } while (stockUnitRepository.existsById(id))
        return id
    }

    private fun parseExpiryToFirstDayOfMonth(s: String): LocalDate? {
        val t = s.trim()
        if (t.isEmpty()) return null
        return when {
            t.length == 7 && t[4] == '-' -> YearMonth.parse(t).atDay(1)
            else -> try {
                LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE).with(TemporalAdjusters.firstDayOfMonth())
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun revertStockForDeliveredPo(poId: Long) {
        val detailsPrefix = "PO #$poId"
        val movements = stockMovementRepository.findByTypeAndDetails(MovementType.ENTRY, detailsPrefix)
        for (movement in movements) {
            val units = stockUnitRepository.findByStockMovementOrderByUnitIndex(movement)
            for (unit in units) {
                stockUnitExpiryRepository.findById(StockUnitExpiryId(movement.id, unit.unitIndex))
                    ?.let { stockUnitExpiryRepository.delete(it) }
                stockUnitRepository.delete(unit)
            }
            stockMovementRepository.delete(movement)
        }
    }

    private fun createStockEntriesForReceivedItems(
        po: PurchaseOrder,
        receivedItems: List<Pair<PurchaseOrderItem, Int>>,
        itemExpiry: Map<String, List<String>>,
    ) {
        val totalReceivedValue = receivedItems.fold(BigDecimal.ZERO) { acc, (item, qty) ->
            acc.add(item.unitCost.multiply(BigDecimal(qty)))
        }
        val freight = po.totalFreight
        for ((item, receivedQty) in receivedItems) {
            item.receivedQuantity = receivedQty
            if (receivedQty <= 0) continue
            val itemValue = item.unitCost.multiply(BigDecimal(receivedQty))
            val freightShare = if (totalReceivedValue.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
            else freight.multiply(itemValue).divide(totalReceivedValue, 2, RoundingMode.HALF_UP)
            val unitPriceWithFreight = item.unitCost.add(
                freightShare.divide(BigDecimal(receivedQty), 2, RoundingMode.HALF_UP),
            )
            val dates = itemExpiry[item.id.toString()]
                ?.mapNotNull { parseExpiryToFirstDayOfMonth(it) }
                .orEmpty()
            val hasValidity = item.product.category.hasValidity
            val movement = StockMovement(
                product = item.product,
                quantity = receivedQty,
                type = MovementType.ENTRY,
                details = "PO #${po.id}",
                purchasePrice = unitPriceWithFreight,
                expiresAt = if (hasValidity && dates.isNotEmpty()) dates.minOrNull() else null,
                variationOption = item.variationOption,
            )
            val savedMovement = stockMovementRepository.save(movement)
            for (idx in 0 until receivedQty) {
                stockUnitRepository.save(
                    StockUnit(id = generateStockUnitId(), stockMovement = savedMovement, unitIndex = idx),
                )
                if (hasValidity && idx < dates.size) {
                    stockUnitExpiryRepository.save(
                        StockUnitExpiry(StockUnitExpiryId(savedMovement.id, idx), expiresAt = dates[idx]),
                    )
                }
            }
            if (hasValidity && dates.isNotEmpty()) {
                item.expiresAt = dates.minOrNull()
            }
            try {
                val summary = stockManagementService.getStockSummary(item.product.id)
                val newAverageCost = summary.averageCost
                stockEntryPricingPort.onPurchaseOrderEntryAdded(
                    item.product.id,
                    newAverageCost,
                    unitPriceWithFreight,
                )
            } catch (e: Exception) {
                log.warn("Price hook failed for product {}: {}", item.product.id, e.message)
            }
        }
    }

    private fun createStockEntriesFromReceivedItemBlocks(
        po: PurchaseOrder,
        receivedItemBlocks: Map<String, List<ReceivedItemBlockDto>>,
    ) {
        val totalReceivedValue = po.items.fold(BigDecimal.ZERO) { acc, item ->
            val blocks = receivedItemBlocks[item.id.toString()].orEmpty()
            val qty = blocks.sumOf { b -> b.quantity }
            acc.add(item.unitCost.multiply(BigDecimal(qty)))
        }
        val freight = po.totalFreight
        for (item in po.items) {
            val blocks = receivedItemBlocks[item.id.toString()].orEmpty().filter { it.quantity > 0 }
            val receivedQty = blocks.sumOf { it.quantity }
            item.receivedQuantity = receivedQty.coerceIn(0, item.quantity)
            for (block in blocks) {
                val variationOption = block.variationOptionId?.let { optId ->
                    variationOptionRepository.findByProductIdAndId(item.product.id, optId)
                }
                val itemValue = item.unitCost.multiply(BigDecimal(block.quantity))
                val freightShare = if (totalReceivedValue.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
                else freight.multiply(itemValue).divide(totalReceivedValue, 2, RoundingMode.HALF_UP)
                val unitPriceWithFreight = item.unitCost.add(
                    freightShare.divide(BigDecimal(block.quantity), 2, RoundingMode.HALF_UP),
                )
                val dates = block.expiryDates.mapNotNull { parseExpiryToFirstDayOfMonth(it) }
                val hasValidity = item.product.category.hasValidity
                val movement = StockMovement(
                    product = item.product,
                    quantity = block.quantity,
                    type = MovementType.ENTRY,
                    details = "PO #${po.id}",
                    purchasePrice = unitPriceWithFreight,
                    expiresAt = if (hasValidity && dates.isNotEmpty()) dates.minOrNull() else null,
                    variationOption = variationOption,
                )
                val savedMovement = stockMovementRepository.save(movement)
                for (idx in 0 until block.quantity) {
                    stockUnitRepository.save(
                        StockUnit(id = generateStockUnitId(), stockMovement = savedMovement, unitIndex = idx),
                    )
                    if (hasValidity && idx < dates.size) {
                        stockUnitExpiryRepository.save(
                            StockUnitExpiry(StockUnitExpiryId(savedMovement.id, idx), expiresAt = dates[idx]),
                        )
                    }
                }
                if (hasValidity && dates.isNotEmpty() && item.expiresAt == null) {
                    item.expiresAt = dates.minOrNull()
                }
                try {
                    val summary = stockManagementService.getStockSummary(item.product.id)
                    stockEntryPricingPort.onPurchaseOrderEntryAdded(
                        item.product.id,
                        summary.averageCost,
                        unitPriceWithFreight,
                    )
                } catch (e: Exception) {
                    log.warn("Price hook failed for product {}: {}", item.product.id, e.message)
                }
            }
        }
    }

    private fun toResponse(po: PurchaseOrder): PurchaseOrderResponse = PurchaseOrderResponse(
        id = po.id,
        totalFreight = po.totalFreight,
        status = po.status.name,
        createdAt = po.createdAt,
        deliveredAt = po.deliveredAt,
        supplierId = po.supplier?.id,
        supplierName = po.supplier?.name,
        items = po.items.map { itemToResponse(it) },
    )

    private fun itemToResponse(item: PurchaseOrderItem): PurchaseOrderItemResponse = PurchaseOrderItemResponse(
        id = item.id,
        productId = item.product.id,
        productName = item.product.name,
        productDescription = item.product.description,
        quantity = item.quantity,
        receivedQuantity = item.receivedQuantity,
        unitCost = item.unitCost,
        expiresAt = item.expiresAt?.toString(),
        categoryHasValidity = item.product.category.hasValidity,
        variationOptionId = item.variationOption?.id,
        variationOptionName = item.variationOption?.name,
    )

    fun getPending(): List<PurchaseOrderResponse> =
        purchaseOrderRepository.findByStatusWithItems(PurchaseOrderStatus.PENDING).map { toResponse(it) }

    fun getAll(): List<PurchaseOrderResponse> =
        purchaseOrderRepository.findAllWithItemsDesc().map { toResponse(it) }

    fun getById(id: Long): PurchaseOrderResponse {
        val po = purchaseOrderRepository.findByIdWithItems(id)
            ?: throw IllegalArgumentException("Purchase order not found: $id")
        return toResponse(po)
    }

    @Transactional
    fun create(dto: CreatePurchaseOrderRequest): PurchaseOrderResponse {
        if (dto.items.isEmpty()) throw IllegalArgumentException("At least one item is required")
        val supplier = dto.supplierId?.let { supplierRepository.findById(it) }
        val po = PurchaseOrder(totalFreight = dto.totalFreight, status = PurchaseOrderStatus.PENDING).apply {
            this.supplier = supplier
        }
        val saved = purchaseOrderRepository.save(po)
        for (it in dto.items) {
            val product = productRepository.findById(it.productId)
                ?: throw IllegalArgumentException("Product not found: ${it.productId}")
            val hasVariations = variationOptionRepository.findByProductId(product.id).isNotEmpty()
            if (hasVariations && it.variationOptionId == null) {
                throw IllegalArgumentException("Produto '${product.name}' possui variações; informe a variante (variationOptionId) no item.")
            }
            val variationOption = it.variationOptionId?.let { optId ->
                variationOptionRepository.findByProductIdAndId(product.id, optId)
                    ?: throw IllegalArgumentException("Variation option $optId not found for product ${product.id}")
            }
            val item = PurchaseOrderItem(
                purchaseOrder = saved,
                product = product,
                quantity = it.quantity,
                unitCost = it.unitCost,
                variationOption = variationOption,
            )
            saved.items.add(item)
        }
        purchaseOrderRepository.save(saved)
        return getById(saved.id)
    }

    @Transactional
    fun update(id: Long, dto: UpdatePurchaseOrderRequest): PurchaseOrderResponse {
        if (dto.items.isEmpty()) throw IllegalArgumentException("At least one item is required")
        val po = purchaseOrderRepository.findByIdWithItems(id)
            ?: throw IllegalArgumentException("Purchase order not found: $id")
        if (po.status == PurchaseOrderStatus.PENDING) {
            po.totalFreight = dto.totalFreight
            po.supplier = dto.supplierId?.let { supplierRepository.findById(it) }
            po.items.clear()
            for (it in dto.items) {
                val product = productRepository.findById(it.productId)
                    ?: throw IllegalArgumentException("Product not found: ${it.productId}")
                val hasVariations = variationOptionRepository.findByProductId(product.id).isNotEmpty()
                if (hasVariations && it.variationOptionId == null) {
                    throw IllegalArgumentException("Produto '${product.name}' possui variações; informe a variante (variationOptionId) no item.")
                }
                val variationOption = it.variationOptionId?.let { optId ->
                    variationOptionRepository.findByProductIdAndId(product.id, optId)
                        ?: throw IllegalArgumentException("Variation option $optId not found for product ${product.id}")
                }
                po.items.add(
                    PurchaseOrderItem(
                        purchaseOrder = po,
                        product = product,
                        quantity = it.quantity,
                        unitCost = it.unitCost,
                        variationOption = variationOption,
                    ),
                )
            }
            purchaseOrderRepository.save(po)
            return getById(po.id)
        }
        if (po.status == PurchaseOrderStatus.DELIVERED) {
            revertStockForDeliveredPo(po.id)
            po.totalFreight = dto.totalFreight
            po.supplier = dto.supplierId?.let { supplierRepository.findById(it) }
            po.items.clear()
            for (it in dto.items) {
                val product = productRepository.findById(it.productId)
                    ?: throw IllegalArgumentException("Product not found: ${it.productId}")
                val hasVariations = variationOptionRepository.findByProductId(product.id).isNotEmpty()
                if (hasVariations && it.variationOptionId == null) {
                    throw IllegalArgumentException("Produto '${product.name}' possui variações; informe a variante (variationOptionId) no item.")
                }
                val variationOption = it.variationOptionId?.let { optId ->
                    variationOptionRepository.findByProductIdAndId(product.id, optId)
                        ?: throw IllegalArgumentException("Variation option $optId not found for product ${product.id}")
                }
                val newItem = PurchaseOrderItem(
                    purchaseOrder = po,
                    product = product,
                    quantity = it.quantity,
                    unitCost = it.unitCost,
                    variationOption = variationOption,
                )
                newItem.receivedQuantity = it.quantity
                po.items.add(newItem)
            }
            purchaseOrderRepository.save(po)
            val receivedItems = po.items.map { it to (it.receivedQuantity ?: it.quantity) }
            createStockEntriesForReceivedItems(po, receivedItems, emptyMap())
            purchaseOrderRepository.save(po)
            return getById(po.id)
        }
        throw IllegalArgumentException("Only pending or delivered purchase orders can be edited")
    }

    @Transactional
    fun cancel(id: Long): PurchaseOrderResponse {
        val po = purchaseOrderRepository.findByIdWithItems(id)
            ?: throw IllegalArgumentException("Purchase order not found: $id")
        if (po.status != PurchaseOrderStatus.PENDING) {
            throw IllegalArgumentException("Only pending purchase orders can be cancelled")
        }
        po.status = PurchaseOrderStatus.CANCELLED
        po.cancelledAt = java.time.LocalDateTime.now()
        purchaseOrderRepository.save(po)
        return getById(po.id)
    }

    @Transactional
    fun delete(id: Long) {
        val po = purchaseOrderRepository.findByIdWithItems(id)
            ?: throw IllegalArgumentException("Purchase order not found: $id")
        if (po.status != PurchaseOrderStatus.DELIVERED) {
            throw IllegalArgumentException("Only delivered purchase orders can be deleted")
        }
        revertStockForDeliveredPo(po.id)
        purchaseOrderRepository.delete(po)
    }

    @Transactional
    fun confirmDelivery(id: Long, dto: ConfirmDeliveryRequest): PurchaseOrderResponse {
        val po = purchaseOrderRepository.findByIdWithItems(id)
            ?: throw IllegalArgumentException("Purchase order not found: $id")
        if (po.status != PurchaseOrderStatus.PENDING) {
            throw IllegalArgumentException("Purchase order is not pending")
        }
        val items = po.items
        if (dto.receivedItemBlocks.isNotEmpty()) {
            for (item in items) {
                val blocks = dto.receivedItemBlocks[item.id.toString()].orEmpty()
                val total = blocks.sumOf { b -> b.quantity.coerceIn(0, item.quantity) }
                if (total > item.quantity) {
                    throw IllegalArgumentException("Item ${item.id}: total received blocks ($total) exceeds ordered quantity (${item.quantity})")
                }
            }
            createStockEntriesFromReceivedItemBlocks(po, dto.receivedItemBlocks)
        } else {
            for (item in items) {
                val optId = dto.itemVariations[item.id.toString()]
                if (optId != null) {
                    val opt = variationOptionRepository.findByProductIdAndId(item.product.id, optId)
                    if (opt != null) item.variationOption = opt
                }
            }
            val receivedItems = items.map { item ->
                val receivedQty = if (dto.receivedQuantities.isNotEmpty()) {
                    dto.receivedQuantities[item.id.toString()] ?: item.quantity
                } else {
                    item.quantity
                }
                item to receivedQty.coerceIn(0, item.quantity)
            }
            createStockEntriesForReceivedItems(po, receivedItems, dto.itemExpiry)
        }
        po.status = PurchaseOrderStatus.DELIVERED
        po.deliveredAt = dto.deliveredAt
        purchaseOrderRepository.save(po)
        return getById(po.id)
    }
}
