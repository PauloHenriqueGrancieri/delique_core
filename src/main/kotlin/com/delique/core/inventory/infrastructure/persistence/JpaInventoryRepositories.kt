package com.delique.core.inventory.infrastructure.persistence

import com.delique.core.inventory.domain.model.Combo
import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.PurchaseOrder
import com.delique.core.inventory.domain.model.PurchaseOrderStatus
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.model.StockUnit
import com.delique.core.inventory.domain.model.StockUnitExpiry
import com.delique.core.inventory.domain.model.StockUnitExpiryId
import com.delique.core.inventory.domain.port.ComboRepository
import com.delique.core.inventory.domain.port.PurchaseOrderRepository
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.inventory.domain.port.StockUnitExpiryRepository
import com.delique.core.inventory.domain.port.StockUnitRepository
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

interface JpaStockMovementJpa : JpaRepository<StockMovement, Long> {
    fun findByProduct(product: Product): List<StockMovement>
    fun findByProductAndType(product: Product, type: MovementType): List<StockMovement>

    @Query(
        "SELECT sm FROM StockMovement sm WHERE sm.product = :product AND sm.type = 'ENTRY' AND sm.quantity > 0 ORDER BY sm.expiresAt ASC NULLS LAST, sm.createdAt ASC",
    )
    fun findEntryLotsForFifo(product: Product): List<StockMovement>

    @Query(
        """
        SELECT sm FROM StockMovement sm
        WHERE sm.product = :product AND sm.type = 'ENTRY' AND sm.quantity > 0
        AND (:variationOption IS NULL AND sm.variationOption IS NULL OR sm.variationOption = :variationOption)
        ORDER BY sm.expiresAt ASC NULLS LAST, sm.createdAt ASC
        """,
    )
    fun findEntryLotsForFifo(
        product: Product,
        @Param("variationOption") variationOption: ProductVariationOption?,
    ): List<StockMovement>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN sm.type = 'ENTRY' THEN sm.quantity WHEN sm.type = 'RETURN' THEN sm.quantity WHEN sm.type = 'DELETE' THEN -sm.quantity ELSE 0 END), 0) FROM StockMovement sm WHERE sm.product = :product",
    )
    fun getCurrentStock(product: Product): Int

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN sm.type = 'ENTRY' THEN sm.quantity WHEN sm.type = 'RETURN' THEN sm.quantity WHEN sm.type = 'DELETE' THEN -sm.quantity ELSE 0 END), 0)
        FROM StockMovement sm
        WHERE sm.product = :product
        AND (:variationOption IS NULL AND sm.variationOption IS NULL OR sm.variationOption = :variationOption)
        """,
    )
    fun getCurrentStockForVariant(
        product: Product,
        @Param("variationOption") variationOption: ProductVariationOption?,
    ): Int

    fun findBySaleId(saleId: Long): List<StockMovement>
    fun findByTypeAndDetails(type: MovementType, details: String): List<StockMovement>

    @Query(
        """
        SELECT sm FROM StockMovement sm
        WHERE sm.product = :product AND sm.type = :type
        AND (:variationOption IS NULL AND sm.variationOption IS NULL OR sm.variationOption = :variationOption)
        """,
    )
    fun findByProductAndTypeAndVariationOption(
        product: Product,
        type: MovementType,
        @Param("variationOption") variationOption: ProductVariationOption?,
    ): List<StockMovement>
}

@Repository
class StockMovementRepositoryAdapter(
    private val jpa: JpaStockMovementJpa,
) : StockMovementRepository {
    override fun save(movement: StockMovement) = jpa.save(movement)
    override fun findById(id: Long) = jpa.findById(id).orElse(null)
    override fun findAllList() = jpa.findAll()
    override fun findAll(pageable: Pageable): Page<StockMovement> = jpa.findAll(pageable)
    override fun findByProduct(product: Product) = jpa.findByProduct(product)
    override fun findByProductAndType(product: Product, type: MovementType) =
        jpa.findByProductAndType(product, type)

    override fun findEntryLotsForFifo(product: Product) = jpa.findEntryLotsForFifo(product)
    override fun findEntryLotsForFifo(product: Product, variationOption: ProductVariationOption?) =
        jpa.findEntryLotsForFifo(product, variationOption)

    override fun getCurrentStock(product: Product) = jpa.getCurrentStock(product)
    override fun getCurrentStockForVariant(product: Product, variationOption: ProductVariationOption?) =
        jpa.getCurrentStockForVariant(product, variationOption)

    override fun findBySaleId(saleId: Long) = jpa.findBySaleId(saleId)
    override fun findByTypeAndDetails(type: MovementType, details: String) =
        jpa.findByTypeAndDetails(type, details)

    override fun findByProductAndTypeAndVariationOption(
        product: Product,
        type: MovementType,
        variationOption: ProductVariationOption?,
    ) = jpa.findByProductAndTypeAndVariationOption(product, type, variationOption)

    override fun delete(movement: StockMovement) = jpa.delete(movement)
    override fun deleteById(id: Long) = jpa.deleteById(id)
}

interface JpaStockUnitJpa : JpaRepository<StockUnit, String> {
    fun findByStockMovementOrderByUnitIndex(movement: StockMovement): List<StockUnit>
    fun findFirstByStockMovementIdAndUnitIndex(stockMovementId: Long, unitIndex: Int): StockUnit?
}

@Repository
class StockUnitRepositoryAdapter(
    private val jpa: JpaStockUnitJpa,
) : StockUnitRepository {
    override fun save(unit: StockUnit) = jpa.save(unit)
    override fun findById(id: String) = jpa.findById(id).orElse(null)
    override fun existsById(id: String) = jpa.existsById(id)
    override fun findByStockMovementOrderByUnitIndex(movement: StockMovement) =
        jpa.findByStockMovementOrderByUnitIndex(movement)

    override fun findFirstByStockMovementIdAndUnitIndex(stockMovementId: Long, unitIndex: Int) =
        jpa.findFirstByStockMovementIdAndUnitIndex(stockMovementId, unitIndex)

    override fun delete(unit: StockUnit) = jpa.delete(unit)
}

interface JpaStockUnitExpiryJpa : JpaRepository<StockUnitExpiry, StockUnitExpiryId> {
    fun findById_StockMovementId(stockMovementId: Long): List<StockUnitExpiry>

    @Query(
        value = "SELECT * FROM stock_unit_expiry e WHERE CAST((date_trunc('month', e.expires_at) + interval '1 month' - interval '1 day') AS date) <= :threshold ORDER BY e.expires_at ASC",
        nativeQuery = true,
    )
    fun findExpiringBefore(threshold: LocalDate): List<StockUnitExpiry>
}

@Repository
class StockUnitExpiryRepositoryAdapter(
    private val jpa: JpaStockUnitExpiryJpa,
) : StockUnitExpiryRepository {
    override fun save(entity: StockUnitExpiry) = jpa.save(entity)
    override fun findById(id: StockUnitExpiryId) = jpa.findById(id).orElse(null)
    override fun findByStockMovementId(stockMovementId: Long) = jpa.findById_StockMovementId(stockMovementId)
    override fun findExpiringBefore(threshold: LocalDate) = jpa.findExpiringBefore(threshold)
    override fun delete(entity: StockUnitExpiry) = jpa.delete(entity)
}

interface JpaPurchaseOrderJpa : JpaRepository<PurchaseOrder, Long> {
    @Query("SELECT DISTINCT o FROM PurchaseOrder o LEFT JOIN FETCH o.items WHERE o.status = :status ORDER BY o.createdAt DESC")
    fun findByStatusWithItems(status: PurchaseOrderStatus): List<PurchaseOrder>

    @Query("SELECT DISTINCT o FROM PurchaseOrder o LEFT JOIN FETCH o.items WHERE o.id = :id")
    fun findByIdWithItems(id: Long): PurchaseOrder?

    @Query("SELECT DISTINCT o FROM PurchaseOrder o LEFT JOIN FETCH o.items ORDER BY o.createdAt DESC")
    fun findAllWithItemsDesc(): List<PurchaseOrder>
}

@Repository
class PurchaseOrderRepositoryAdapter(
    private val jpa: JpaPurchaseOrderJpa,
) : PurchaseOrderRepository {
    override fun save(order: PurchaseOrder) = jpa.save(order)
    override fun findById(id: Long) = jpa.findById(id).orElse(null)
    override fun findByIdWithItems(id: Long) = jpa.findByIdWithItems(id)
    override fun findByStatusWithItems(status: PurchaseOrderStatus) = jpa.findByStatusWithItems(status)
    override fun findAllWithItemsDesc() = jpa.findAllWithItemsDesc()
    override fun delete(order: PurchaseOrder) = jpa.delete(order)
}

interface JpaComboJpa : JpaRepository<Combo, Long> {
    fun findByProduct_Id(productId: Long): Combo?

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items WHERE c.product.id = :productId")
    fun findByProductIdWithItems(@Param("productId") productId: Long): Combo?

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items")
    fun findAllWithItems(): List<Combo>

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items WHERE c.active = :active")
    fun findAllByActiveWithItems(@Param("active") active: Boolean): List<Combo>

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items WHERE c.id = :id")
    fun findByIdWithItems(@Param("id") id: Long): Combo?
}

@Repository
class ComboRepositoryAdapter(
    private val jpa: JpaComboJpa,
) : ComboRepository {
    override fun save(combo: Combo) = jpa.save(combo)
    override fun findById(id: Long) = jpa.findById(id).orElse(null)
    override fun findByIdWithItems(id: Long) = jpa.findByIdWithItems(id)
    override fun findByProductId(productId: Long) = jpa.findByProduct_Id(productId)
    override fun findByProductIdWithItems(productId: Long) = jpa.findByProductIdWithItems(productId)
    override fun findAllWithItems() = jpa.findAllWithItems()
    override fun findAllByActiveWithItems(active: Boolean) = jpa.findAllByActiveWithItems(active)
}
