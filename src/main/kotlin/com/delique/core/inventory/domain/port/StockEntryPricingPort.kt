package com.delique.core.inventory.domain.port

import java.math.BigDecimal

/**
 * Notifies pricing/catalog integration after stock changes (pending price calculation queue).
 */
interface StockEntryPricingPort {
    fun onManualEntryAdded(productId: Long, newAverageCost: BigDecimal, entryPurchasePrice: BigDecimal?)
    fun onPurchaseOrderEntryAdded(
        productId: Long,
        newAverageCost: BigDecimal,
        unitCostWithFreight: BigDecimal,
    )
}
