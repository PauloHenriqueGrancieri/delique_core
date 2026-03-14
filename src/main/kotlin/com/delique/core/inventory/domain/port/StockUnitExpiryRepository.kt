package com.delique.core.inventory.domain.port

import com.delique.core.inventory.domain.model.StockUnitExpiry
import com.delique.core.inventory.domain.model.StockUnitExpiryId
import java.time.LocalDate

interface StockUnitExpiryRepository {
    fun save(entity: StockUnitExpiry): StockUnitExpiry
    fun findById(id: StockUnitExpiryId): StockUnitExpiry?
    fun findByStockMovementId(stockMovementId: Long): List<StockUnitExpiry>
    fun findExpiringBefore(threshold: LocalDate): List<StockUnitExpiry>
    fun delete(entity: StockUnitExpiry)
}
