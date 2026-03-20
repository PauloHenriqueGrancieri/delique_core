package com.delique.core.inventory.application

import com.delique.core.inventory.application.dto.*
import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.model.StockUnit
import com.delique.core.inventory.domain.model.StockUnitExpiry
import com.delique.core.inventory.domain.model.StockUnitExpiryId
import com.delique.core.inventory.domain.port.StockEntryPricingPort
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.inventory.domain.port.StockUnitExpiryRepository
import com.delique.core.inventory.domain.port.StockUnitRepository
import com.delique.core.inventory.domain.service.StockDomainService
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.product.domain.port.ProductVariationOptionRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class StockManagementService(
    private val stockMovementRepository: StockMovementRepository,
    private val stockUnitRepository: StockUnitRepository,
    private val stockUnitExpiryRepository: StockUnitExpiryRepository,
    private val productRepository: ProductRepository,
    private val variationOptionRepository: ProductVariationOptionRepository,
    private val stockDomainService: StockDomainService,
    @Lazy private val stockEntryPricingPort: StockEntryPricingPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun generateStockUnitId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString()
        } while (stockUnitRepository.existsById(id))
        return id
    }

    fun getProductEntity(productId: Long): Product =
        productRepository.findById(productId) ?: throw IllegalArgumentException("Product not found: $productId")

    fun getAllMovements(): List<StockMovementResponse> =
        stockMovementRepository.findAllList().map { it.toResponse() }

    fun getMovementsByProduct(productId: Long): List<StockMovementResponse> {
        val product = getProductEntity(productId)
        return stockMovementRepository.findByProduct(product).map { it.toResponse() }
    }

    fun getCurrentStock(productId: Long): Int {
        val product = getProductEntity(productId)
        return maxOf(0, stockMovementRepository.getCurrentStock(product))
    }

    fun getCurrentStock(productId: Long, variationOptionId: Long?): Int {
        val product = getProductEntity(productId)
        val variationOption = variationOptionId?.let {
            variationOptionRepository.findByProductIdAndId(product.id, it)
        }
        return maxOf(0, stockMovementRepository.getCurrentStockForVariant(product, variationOption))
    }

    fun getStockHistory(page: Int, size: Int): Page<StockMovementResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return stockMovementRepository.findAll(pageable).map { it.toResponse() }
    }

    @Transactional
    fun getStockSummary(productId: Long): StockSummaryResponse =
        buildSummary(getProductEntity(productId), null)

    @Transactional
    fun getStockSummary(productId: Long, variationOptionId: Long?): StockSummaryResponse {
        val product = getProductEntity(productId)
        val variationOption = variationOptionId?.let {
            variationOptionRepository.findByProductIdAndId(product.id, it)
        }
        return buildSummary(product, variationOption)
    }

    private fun buildSummary(product: Product, variationOption: ProductVariationOption?): StockSummaryResponse {
        val entries = stockMovementRepository.findByProductAndTypeAndVariationOption(
            product,
            MovementType.ENTRY,
            variationOption,
        ).filter { it.quantity > 0 }
        stockDomainService.ensureStockUnitsForEntries(entries, ::generateStockUnitId)
        val entryQuantity = entries.sumOf { it.quantity }
        val entryTotalCost = entries
            .filter { it.purchasePrice != null }
            .sumOf { it.purchasePrice!!.multiply(BigDecimal(it.quantity)) }
        val averageCost = if (entryQuantity > 0 && entryTotalCost > BigDecimal.ZERO) {
            entryTotalCost.divide(BigDecimal(entryQuantity), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val currentQuantity = if (variationOption == null) {
            stockMovementRepository.getCurrentStock(product)
        } else {
            stockMovementRepository.getCurrentStockForVariant(product, variationOption)
        }
        val totalCost = averageCost.multiply(BigDecimal(currentQuantity)).setScale(2, RoundingMode.HALF_UP)
        val movementIds = entries.map { it.id }
        val overrides = movementIds.flatMap { stockUnitExpiryRepository.findByStockMovementId(it) }
            .associate { (it.id.stockMovementId to it.id.unitIndex) to it.expiresAt }
        val formatted = stockDomainService.formattedProductCode(product)
        var unitOrdinal = 0
        val batches = entries
            .sortedWith(compareBy<StockMovement> { it.expiresAt ?: LocalDate.MAX }.thenBy { it.createdAt })
            .map { entry ->
                val units = stockUnitRepository.findByStockMovementOrderByUnitIndex(entry).map { su ->
                    unitOrdinal++
                    val expiresAt = overrides[entry.id to su.unitIndex] ?: entry.expiresAt
                    StockUnitResponse(
                        stockUnitId = su.id,
                        unitIndex = su.unitIndex,
                        movementId = entry.id,
                        expiresAt = expiresAt,
                        createdAt = entry.createdAt,
                        purchasePrice = entry.purchasePrice,
                        productCode = formatted ?: "",
                        productUnitNumber = unitOrdinal,
                    )
                }
                StockBatchResponse(
                    movementId = entry.id,
                    quantity = entry.quantity,
                    purchasePrice = entry.purchasePrice,
                    createdAt = entry.createdAt,
                    units = units,
                )
            }
        val allUnits = batches.flatMap { it.units }.sortedWith(
            compareBy<StockUnitResponse> { it.expiresAt ?: LocalDate.MAX }
                .thenBy { it.createdAt }
                .thenBy { it.unitIndex },
        )
        return StockSummaryResponse(
            productId = product.id,
            productName = product.name,
            productDescription = product.description,
            formattedDisplayId = formatted,
            totalQuantity = currentQuantity,
            totalCost = totalCost,
            averageCost = averageCost,
            batches = batches,
            units = allUnits,
            variationOptionId = variationOption?.id,
            variationOptionName = variationOption?.name,
        )
    }

    fun getGroupedStock(): List<StockSummaryResponse> {
        val result = mutableListOf<StockSummaryResponse>()
        for (product in productRepository.findAllOrdered()) {
            val entries = stockMovementRepository.findByProductAndType(product, MovementType.ENTRY)
                .filter { it.quantity > 0 }
            if (entries.isEmpty()) continue
            val variantIds = entries.map { it.variationOption?.id }.distinct()
            for (vid in variantIds) {
                val summary = getStockSummary(product.id, vid)
                if (summary.totalQuantity > 0) result.add(summary)
            }
        }
        return result
    }

    @Transactional
    fun addStockEntry(dto: StockMovementRequest): StockMovementResponse {
        val product = getProductEntity(dto.productId)
        val hasValidity = product.category.hasValidity
        if (hasValidity && dto.expiresAt == null) {
            log.warn("Stock entry rejected: product requires expiry")
            throw IllegalArgumentException("Este produto requer data de validade")
        }
        val normalizedExpiry = stockDomainService.normalizeExpiryToFirstDayOfMonth(dto.expiresAt)
        val variationOption = dto.variationOptionId?.let {
            variationOptionRepository.findByProductIdAndId(product.id, it)
        }
        val movement = StockMovement(
            product = product,
            quantity = dto.quantity,
            type = MovementType.ENTRY,
            details = dto.details,
            purchasePrice = dto.purchasePrice,
            expiresAt = normalizedExpiry,
            variationOption = variationOption,
        )
        val saved = stockMovementRepository.save(movement)
        for (idx in 0 until saved.quantity) {
            stockUnitRepository.save(StockUnit(id = generateStockUnitId(), stockMovement = saved, unitIndex = idx))
            if (normalizedExpiry != null) {
                stockUnitExpiryRepository.save(
                    StockUnitExpiry(StockUnitExpiryId(saved.id, idx), expiresAt = normalizedExpiry),
                )
            }
        }
        if (dto.purchasePrice != null && dto.purchasePrice > BigDecimal.ZERO) {
            val summary = getStockSummary(product.id)
            stockEntryPricingPort.onManualEntryAdded(product.id, summary.averageCost, dto.purchasePrice)
        }
        return saved.toResponse()
    }

    @Transactional
    fun consumeStockFifo(productId: Long, quantity: Int, variationOptionId: Long? = null) {
        val product = getProductEntity(productId)
        val variationOption = variationOptionId?.let {
            variationOptionRepository.findByProductIdAndId(product.id, it)
        }
        val lots = stockMovementRepository.findEntryLotsForFifo(product, variationOption)
        val movementIds = lots.map { it.id }
        val overrides = movementIds.flatMap { stockUnitExpiryRepository.findByStockMovementId(it) }
            .associate { (it.id.stockMovementId to it.id.unitIndex) to it.expiresAt }
        stockDomainService.consumeFifo(product, variationOption, quantity, overrides)
    }

    @Transactional
    fun restoreStockFromSale(saleId: Long) {
        stockDomainService.restoreStockFromSale(saleId)
    }

    @Transactional
    fun deleteStock(dto: StockMovementRequest): StockMovementResponse {
        val product = getProductEntity(dto.productId)
        val movement = StockMovement(
            product = product,
            quantity = dto.quantity,
            type = MovementType.DELETE,
            details = dto.details,
        )
        return stockMovementRepository.save(movement).toResponse()
    }

    @Transactional
    fun deleteStockByIds(ids: List<String>, reason: String?): Map<String, Any> {
        val uuids = ids.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (uuids.isEmpty()) {
            throw IllegalArgumentException("Informe pelo menos um UUID do estoque para exclusão.")
        }
        val productRemoved = mutableMapOf<Long, Int>()
        for (stockUnitId in uuids) {
            val unit = stockUnitRepository.findById(stockUnitId) ?: continue
            val movement = unit.stockMovement
            stockUnitExpiryRepository.findById(StockUnitExpiryId(movement.id, unit.unitIndex))
                ?.let { stockUnitExpiryRepository.delete(it) }
            stockUnitRepository.delete(unit)
            movement.quantity -= 1
            stockMovementRepository.save(movement)
            val productId = movement.product.id
            productRemoved[productId] = (productRemoved[productId] ?: 0) + 1
        }
        return mapOf(
            "deletedCount" to productRemoved.values.sum(),
            "byProduct" to productRemoved,
        )
    }

    @Transactional
    fun updateUnitExpiry(movementId: Long, unitIndex: Int, expiresAt: LocalDate?): StockEntryResponse {
        val movement = stockMovementRepository.findById(movementId)
            ?: throw IllegalArgumentException("Stock movement not found: $movementId")
        if (movement.type != MovementType.ENTRY) {
            throw IllegalArgumentException("Movement $movementId is not an entry")
        }
        if (unitIndex < 0) {
            throw IllegalArgumentException("Invalid unit index: $unitIndex")
        }
        val stockUnitExists = stockUnitRepository.findFirstByStockMovementIdAndUnitIndex(movementId, unitIndex) != null
        if (!stockUnitExists && unitIndex >= movement.quantity) {
            throw IllegalArgumentException("Invalid unit index $unitIndex for lot $movementId (qty ${movement.quantity})")
        }
        val id = StockUnitExpiryId(movementId, unitIndex)
        val resultDate: LocalDate? = if (expiresAt != null) {
            val normalized = stockDomainService.normalizeExpiryToFirstDayOfMonth(expiresAt)!!
            val existing = stockUnitExpiryRepository.findById(id)
            if (existing != null) {
                existing.expiresAt = normalized
                stockUnitExpiryRepository.save(existing)
            } else {
                stockUnitExpiryRepository.save(StockUnitExpiry(id = id, expiresAt = normalized))
            }
            normalized
        } else {
            stockUnitExpiryRepository.findById(id)?.let { stockUnitExpiryRepository.delete(it) }
            null
        }
        val stockUnit = stockUnitRepository.findFirstByStockMovementIdAndUnitIndex(movementId, unitIndex)
        return StockEntryResponse(
            id = movement.id,
            quantity = 1,
            purchasePrice = movement.purchasePrice,
            createdAt = movement.createdAt,
            expiresAt = resultDate,
            unitIndex = unitIndex,
            stockUnitId = stockUnit?.id,
        )
    }

    @Transactional
    fun updateUnitExpiryByStockUnitId(stockUnitId: String, expiresAt: LocalDate?): StockEntryResponse {
        val unit = stockUnitRepository.findById(stockUnitId)
            ?: throw IllegalArgumentException("Stock unit not found: $stockUnitId")
        return updateUnitExpiry(unit.stockMovement.id, unit.unitIndex, expiresAt)
    }

    @Transactional
    fun editMovement(movementId: Long, newQuantity: Int, reason: String): StockMovementResponse {
        val movement = stockMovementRepository.findById(movementId)
            ?: throw IllegalArgumentException("Stock movement not found with id: $movementId")
        val oldQuantity = movement.quantity
        val timestamp = LocalDateTime.now()
        when {
            newQuantity > oldQuantity -> {
                for (idx in oldQuantity until newQuantity) {
                    stockUnitRepository.save(
                        StockUnit(id = generateStockUnitId(), stockMovement = movement, unitIndex = idx),
                    )
                    movement.expiresAt?.let { exp ->
                        val normalized = stockDomainService.normalizeExpiryToFirstDayOfMonth(exp)!!
                        stockUnitExpiryRepository.save(
                            StockUnitExpiry(StockUnitExpiryId(movement.id, idx), expiresAt = normalized),
                        )
                    }
                }
            }
            newQuantity < oldQuantity -> {
                val unitsToRemove = stockUnitRepository.findByStockMovementOrderByUnitIndex(movement)
                    .sortedByDescending { it.unitIndex }
                    .take(oldQuantity - newQuantity)
                for (unit in unitsToRemove) {
                    stockUnitExpiryRepository.findById(StockUnitExpiryId(movement.id, unit.unitIndex))
                        ?.let { stockUnitExpiryRepository.delete(it) }
                    stockUnitRepository.delete(unit)
                }
            }
        }
        movement.quantity = newQuantity
        movement.editedAt = timestamp
        movement.editReason = "Original quantity: $oldQuantity. New quantity: $newQuantity. Reason: $reason"
        val saved = stockMovementRepository.save(movement)
        val auditDetails =
            "EDIT AUDIT - Movement #$movementId edited at $timestamp. Changed from $oldQuantity to $newQuantity. Reason: $reason"
        val auditMovement = StockMovement(
            product = movement.product,
            quantity = 0,
            type = MovementType.ENTRY,
            details = auditDetails,
            createdAt = timestamp,
        )
        stockMovementRepository.save(auditMovement)
        return saved.toResponse()
    }

    fun getProductsBelowMinimumStock(): List<StockAlertResponse> =
        productRepository.findByMinimumStockIsNotNull().mapNotNull { product ->
            val current = getCurrentStock(product.id)
            val minimum = product.minimumStock!!
            if (current < minimum) {
                StockAlertResponse(product.id, product.name, current, minimum)
            } else {
                null
            }
        }

    fun getExpiringStock(monthsThreshold: Int): List<ExpiringStockResponse> {
        val threshold = LocalDate.now().plusMonths(monthsThreshold.toLong())
            .with(TemporalAdjusters.lastDayOfMonth())
        val expiries = stockUnitExpiryRepository.findExpiringBefore(threshold)
        val today = LocalDate.now()
        return expiries.mapNotNull { expiry ->
            val movement = stockMovementRepository.findById(expiry.id.stockMovementId) ?: return@mapNotNull null
            if (movement.quantity <= 0) return@mapNotNull null
            val lastDayOfMonth = expiry.expiresAt.with(TemporalAdjusters.lastDayOfMonth())
            ExpiringStockResponse(
                productId = movement.product.id,
                productName = movement.product.name,
                movementId = movement.id,
                unitIndex = expiry.id.unitIndex,
                expiresAt = expiry.expiresAt,
                daysRemaining = ChronoUnit.DAYS.between(today, lastDayOfMonth).toInt(),
            )
        }
    }

    @Transactional
    fun updateMovementVariation(movementId: Long, variationOptionId: Long?): StockMovementResponse {
        val movement = stockMovementRepository.findById(movementId)
            ?: throw IllegalArgumentException("Stock movement not found: $movementId")
        if (movement.type != MovementType.ENTRY) {
            throw IllegalArgumentException("Only ENTRY movements can have their variation updated")
        }
        val variationOption = variationOptionId?.let {
            variationOptionRepository.findByProductIdAndId(movement.product.id, it)
                ?: throw IllegalArgumentException("Variation option $it not found for product ${movement.product.id}")
        }
        movement.variationOption = variationOption
        return stockMovementRepository.save(movement).toResponse()
    }

    @Transactional
    fun updateUnitVariation(stockUnitId: String, variationOptionId: Long?): StockMovementResponse {
        val unit = stockUnitRepository.findById(stockUnitId)
            ?: throw IllegalArgumentException("Stock unit not found: $stockUnitId")
        val movement = unit.stockMovement
        if (movement.type != MovementType.ENTRY) {
            throw IllegalArgumentException("Only ENTRY units can have their variation updated")
        }
        val variationOption = variationOptionId?.let {
            variationOptionRepository.findByProductIdAndId(movement.product.id, it)
                ?: throw IllegalArgumentException("Variation option $it not found for product ${movement.product.id}")
        }
        if (movement.variationOption?.id == variationOption?.id) {
            return movement.toResponse()
        }
        return if (movement.quantity == 1) {
            movement.variationOption = variationOption
            stockMovementRepository.save(movement).toResponse()
        } else {
            val unitExpiry = stockUnitExpiryRepository.findById(StockUnitExpiryId(movement.id, unit.unitIndex))
            val newMovement = StockMovement(
                product = movement.product,
                quantity = 1,
                type = MovementType.ENTRY,
                details = "Split from #${movement.id}",
                purchasePrice = movement.purchasePrice,
                expiresAt = unitExpiry?.expiresAt ?: movement.expiresAt,
                variationOption = variationOption,
            )
            val savedNew = stockMovementRepository.save(newMovement)
            stockUnitRepository.save(
                StockUnit(id = generateStockUnitId(), stockMovement = savedNew, unitIndex = 0),
            )
            unitExpiry?.let { expiry ->
                stockUnitExpiryRepository.delete(expiry)
                stockUnitExpiryRepository.save(
                    StockUnitExpiry(StockUnitExpiryId(savedNew.id, 0), expiresAt = expiry.expiresAt),
                )
            }
            stockUnitRepository.delete(unit)
            movement.quantity -= 1
            stockMovementRepository.save(movement)
            savedNew.toResponse()
        }
    }

    private fun StockMovement.toResponse() = StockMovementResponse(
        id = this.id,
        productId = this.product.id,
        quantity = this.quantity,
        type = this.type,
        details = this.details,
        purchasePrice = this.purchasePrice,
        saleId = this.saleId,
        editedAt = this.editedAt,
        editReason = this.editReason,
        createdAt = this.createdAt,
        expiresAt = this.expiresAt,
        variationOptionId = this.variationOption?.id,
        variationOptionName = this.variationOption?.name,
    )
}
