package com.delique.core.inventory.infrastructure.integration

import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.product.domain.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface JpaCatalogEntryJpa : JpaRepository<CatalogEntry, Long> {
    fun findByProduct_Id(productId: Long): CatalogEntry?
}

interface JpaPriceCalculationConfigJpa : JpaRepository<PriceCalculationConfigRow, Long> {
    fun findFirstByOrderByIdAsc(): PriceCalculationConfigRow?
}

interface JpaPendingPriceCalculationJpa : JpaRepository<PendingPriceCalculationRow, Long> {
    fun findByProduct_Id(productId: Long): PendingPriceCalculationRow?
}

interface JpaNativeStatsJpa : JpaRepository<StockMovement, Long> {
    @Query(value = "SELECT COUNT(*) FROM orders", nativeQuery = true)
    fun countOrders(): Long

    @Query(value = "SELECT COUNT(*) FROM sales", nativeQuery = true)
    fun countSales(): Long
}
