package com.delique.core.inventory.infrastructure.integration

import com.delique.core.pricing.application.PriceCalculationApplicationService
import com.delique.core.pricing.application.dto.PriceCalculationRequest
import com.delique.core.pricing.application.dto.PriceCalculationResponse
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CatalogPricingSupport(
    private val priceCalculationApplicationService: PriceCalculationApplicationService,
) {
    fun calculatePrice(productId: Long?, cmv: BigDecimal): PriceCalculationResponse =
        priceCalculationApplicationService.calculatePrice(
            PriceCalculationRequest(productId = productId, cmv = cmv),
        )
}
