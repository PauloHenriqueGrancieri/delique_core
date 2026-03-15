package com.delique.core.inventory.infrastructure.integration

import com.delique.core.inventory.domain.port.StockEntryPricingPort
import com.delique.core.product.domain.port.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class StockEntryPricingAdapter(
    private val catalogJpa: JpaCatalogEntryJpa,
    private val pendingJpa: JpaPendingPriceCalculationJpa,
    private val productRepository: ProductRepository,
    private val catalogPricingSupport: CatalogPricingSupport,
) : StockEntryPricingPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onManualEntryAdded(productId: Long, newAverageCost: BigDecimal, entryPurchasePrice: BigDecimal?) {
        try {
            if (entryPurchasePrice == null || entryPurchasePrice <= BigDecimal.ZERO) return
            val existingCatalog = catalogJpa.findByProduct_Id(productId) ?: return
            if (existingCatalog.costPrice == null || newAverageCost <= existingCatalog.costPrice!!) return
            val response = catalogPricingSupport.calculatePrice(productId, newAverageCost)
            createPending(productId, existingCatalog.salePrice, response.calculatedPrice, newAverageCost)
        } catch (e: Exception) {
            log.warn("Price side-effect skipped for product {}: {}", productId, e.message)
        }
    }

    override fun onPurchaseOrderEntryAdded(
        productId: Long,
        newAverageCost: BigDecimal,
        unitCostWithFreight: BigDecimal,
    ) {
        try {
            val existingCatalog = catalogJpa.findByProduct_Id(productId)
            val needsPriceCalculation = existingCatalog == null || existingCatalog.salePrice == BigDecimal.ZERO
            val needsRecalculation = existingCatalog != null &&
                existingCatalog.costPrice != null &&
                newAverageCost > existingCatalog.costPrice!!
            if (!needsPriceCalculation && !needsRecalculation) return
            val cmv = if (newAverageCost > BigDecimal.ZERO) newAverageCost else unitCostWithFreight
            if (cmv <= BigDecimal.ZERO) return
            val response = catalogPricingSupport.calculatePrice(productId, cmv)
            when {
                needsPriceCalculation ->
                    createPending(productId, BigDecimal.ZERO, response.calculatedPrice, cmv)
                needsRecalculation && existingCatalog != null ->
                    createPending(productId, existingCatalog.salePrice, response.calculatedPrice, cmv)
            }
        } catch (e: Exception) {
            log.warn("PO price side-effect skipped for product {}: {}", productId, e.message)
        }
    }

    private fun createPending(
        productId: Long,
        currentSalePrice: BigDecimal,
        calculatedPrice: BigDecimal,
        cmv: BigDecimal,
    ) {
        val product = productRepository.findById(productId) ?: return
        val existing = pendingJpa.findByProduct_Id(productId)
        val row = existing ?: PendingPriceCalculationRow(
            product = product,
            currentSalePrice = currentSalePrice,
            calculatedPrice = calculatedPrice,
            finalPrice = calculatedPrice,
            cmv = cmv,
        )
        row.currentSalePrice = currentSalePrice
        row.calculatedPrice = calculatedPrice
        row.finalPrice = calculatedPrice
        row.cmv = cmv
        pendingJpa.save(row)
    }
}
