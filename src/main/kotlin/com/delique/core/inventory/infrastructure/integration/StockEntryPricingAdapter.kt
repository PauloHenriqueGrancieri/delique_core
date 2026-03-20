package com.delique.core.inventory.infrastructure.integration

import com.delique.core.catalog.infrastructure.persistence.CatalogJpa
import com.delique.core.inventory.domain.port.StockEntryPricingPort
import com.delique.core.pricing.application.PriceCalculationApplicationService
import com.delique.core.pricing.application.dto.PriceCalculationRequest
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class StockEntryPricingAdapter(
    private val catalogJpa: CatalogJpa,
    @Lazy private val priceCalculationApplicationService: PriceCalculationApplicationService,
) : StockEntryPricingPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onManualEntryAdded(productId: Long, newAverageCost: BigDecimal, entryPurchasePrice: BigDecimal?) {
        try {
            if (entryPurchasePrice == null || entryPurchasePrice <= BigDecimal.ZERO) return
            val existingCatalog = catalogJpa.findByProduct_Id(productId) ?: return
            if (existingCatalog.costPrice == null || newAverageCost <= existingCatalog.costPrice!!) return
            val response = priceCalculationApplicationService.calculatePrice(
                PriceCalculationRequest(productId = productId, cmv = newAverageCost),
            )
            priceCalculationApplicationService.createPendingPriceCalculation(
                productId,
                existingCatalog.salePrice,
                response.calculatedPrice,
                newAverageCost,
            )
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
            val response = priceCalculationApplicationService.calculatePrice(
                PriceCalculationRequest(productId = productId, cmv = cmv),
            )
            when {
                needsPriceCalculation ->
                    priceCalculationApplicationService.createPendingPriceCalculation(
                        productId,
                        BigDecimal.ZERO,
                        response.calculatedPrice,
                        cmv,
                    )
                needsRecalculation && existingCatalog != null ->
                    priceCalculationApplicationService.createPendingPriceCalculation(
                        productId,
                        existingCatalog.salePrice,
                        response.calculatedPrice,
                        cmv,
                    )
            }
        } catch (e: Exception) {
            log.warn("PO price side-effect skipped for product {}: {}", productId, e.message)
        }
    }
}
