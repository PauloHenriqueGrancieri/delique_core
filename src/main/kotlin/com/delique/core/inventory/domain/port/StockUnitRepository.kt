package com.delique.core.inventory.domain.port

import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.model.StockUnit

interface StockUnitRepository {
    fun save(unit: StockUnit): StockUnit
    fun findById(id: String): StockUnit?
    fun existsById(id: String): Boolean
    fun findByStockMovementOrderByUnitIndex(movement: StockMovement): List<StockUnit>
    fun findFirstByStockMovementIdAndUnitIndex(stockMovementId: Long, unitIndex: Int): StockUnit?
    fun delete(unit: StockUnit)
}
