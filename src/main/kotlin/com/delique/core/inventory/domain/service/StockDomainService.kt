package com.delique.core.inventory.domain.service

import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.model.StockUnit
import com.delique.core.inventory.domain.model.StockUnitExpiry
import com.delique.core.inventory.domain.model.StockUnitExpiryId
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.inventory.domain.port.StockUnitExpiryRepository
import com.delique.core.inventory.domain.port.StockUnitRepository
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class StockDomainService(
    private val stockMovementRepository: StockMovementRepository,
    private val stockUnitRepository: StockUnitRepository,
    private val stockUnitExpiryRepository: StockUnitExpiryRepository,
) {

    fun normalizeExpiryToFirstDayOfMonth(date: LocalDate?): LocalDate? =
        date?.with(TemporalAdjusters.firstDayOfMonth())

    fun ensureStockUnitsForEntries(entries: List<StockMovement>, generateId: () -> String) {
        for (entry in entries) {
            val existingUnits = stockUnitRepository.findByStockMovementOrderByUnitIndex(entry)
            val missing = entry.quantity - existingUnits.size
            if (missing <= 0) continue
            val nextIndex = existingUnits.maxOfOrNull { it.unitIndex }?.plus(1) ?: 0
            for (i in 0 until missing) {
                val unitIndex = nextIndex + i
                stockUnitRepository.save(
                    StockUnit(id = generateId(), stockMovement = entry, unitIndex = unitIndex),
                )
                entry.expiresAt?.let { expiresAt ->
                    val normalized = normalizeExpiryToFirstDayOfMonth(expiresAt)!!
                    stockUnitExpiryRepository.save(
                        StockUnitExpiry(
                            id = StockUnitExpiryId(entry.id, unitIndex),
                            expiresAt = normalized,
                        ),
                    )
                }
            }
        }
    }

    fun formattedProductCode(product: Product): String? {
        val catId = product.category.displayId?.let { String.format("%02d", it) }
        val brandId = product.brand.displayId?.let { String.format("%02d", it) }
        val prodId = product.displayId?.let { String.format("%03d", it) }
        return if (catId != null && brandId != null && prodId != null) catId + brandId + prodId else null
    }

    fun consumeFifo(
        product: Product,
        variationOption: ProductVariationOption?,
        quantity: Int,
        overrides: Map<Pair<Long, Int>, LocalDate>,
    ) {
        val lots = stockMovementRepository.findEntryLotsForFifo(product, variationOption)
        var remaining = quantity
        for (lot in lots) {
            if (remaining <= 0) break
            val take = minOf(lot.quantity, remaining)
            if (take <= 0) continue
            val unitsInLot = stockUnitRepository.findByStockMovementOrderByUnitIndex(lot)
            val sortedUnits = unitsInLot.sortedWith(
                compareBy<StockUnit> { overrides[lot.id to it.unitIndex] ?: lot.expiresAt ?: LocalDate.MAX }
                    .thenBy { it.unitIndex },
            )
            val toDelete = sortedUnits.take(take)
            for (unit in toDelete) {
                stockUnitExpiryRepository.findById(
                    StockUnitExpiryId(lot.id, unit.unitIndex),
                )?.let { stockUnitExpiryRepository.delete(it) }
                stockUnitRepository.delete(unit)
            }
            lot.quantity -= take
            remaining -= take
            stockMovementRepository.save(lot)
        }
        if (remaining > 0) {
            val variantMsg = if (variationOption != null) " variant ${variationOption.id}" else ""
            error("Insufficient stock for product ${product.id}$variantMsg: could not consume $quantity units (FIFO)")
        }
    }

    fun restoreStockFromSale(saleId: Long) {
        val movements = stockMovementRepository.findBySaleId(saleId)
        for (mv in movements) {
            if (mv.type == MovementType.SALE) {
                val entries = stockMovementRepository.findByProductAndTypeAndVariationOption(
                    mv.product,
                    MovementType.ENTRY,
                    mv.variationOption,
                ).sortedByDescending { it.createdAt }
                if (entries.isNotEmpty()) {
                    entries[0].quantity += mv.quantity
                    stockMovementRepository.save(entries[0])
                }
                stockMovementRepository.delete(mv)
            }
        }
    }
}
