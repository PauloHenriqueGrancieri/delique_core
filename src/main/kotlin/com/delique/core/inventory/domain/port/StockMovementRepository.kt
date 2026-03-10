package com.delique.core.inventory.domain.port

import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StockMovementRepository {
    fun save(movement: StockMovement): StockMovement
    fun findById(id: Long): StockMovement?
    fun findAllList(): List<StockMovement>
    fun findAll(pageable: Pageable): Page<StockMovement>
    fun findByProduct(product: Product): List<StockMovement>
    fun findByProductAndType(product: Product, type: MovementType): List<StockMovement>
    fun findEntryLotsForFifo(product: Product): List<StockMovement>
    fun findEntryLotsForFifo(product: Product, variationOption: ProductVariationOption?): List<StockMovement>
    fun getCurrentStock(product: Product): Int
    fun getCurrentStockForVariant(product: Product, variationOption: ProductVariationOption?): Int
    fun findBySaleId(saleId: Long): List<StockMovement>
    fun findByTypeAndDetails(type: MovementType, details: String): List<StockMovement>
    fun findByProductAndTypeAndVariationOption(
        product: Product,
        type: MovementType,
        variationOption: ProductVariationOption?,
    ): List<StockMovement>

    /** Latest inbound movement with purchase price per product (for promotions). */
    fun findLastPurchasePricePerProduct(): List<StockMovement>

    fun delete(movement: StockMovement)
    fun deleteById(id: Long)
}
