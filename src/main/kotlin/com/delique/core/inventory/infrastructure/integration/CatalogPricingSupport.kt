package com.delique.core.inventory.infrastructure.integration

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CatalogPricingSupport(
    private val configJpa: JpaPriceCalculationConfigJpa,
    private val statsJpa: JpaNativeStatsJpa,
) {
    fun defaultConfig(): PriceCalculationConfigRow {
        val c = configJpa.findFirstByOrderByIdAsc() ?: PriceCalculationConfigRow()
        val orders = statsJpa.countOrders()
        if (orders == 0L) {
            c.defaultAverageItemsPerOrder = BigDecimal.ONE
        } else {
            val sales = statsJpa.countSales()
            c.defaultAverageItemsPerOrder = if (sales == 0L) BigDecimal.ONE
            else BigDecimal(sales).divide(BigDecimal(orders), 2, java.math.RoundingMode.HALF_UP)
        }
        return c
    }

    fun calculatePrice(productId: Long?, cmv: BigDecimal): PriceCalculationResult {
        val c = defaultConfig()
        val input = PriceCalculationInput(
            productId = productId,
            cmv = cmv,
            lossPercentage = c.defaultLossPercentage,
            salesCommissionPercentage = c.defaultSalesCommissionPercentage,
            cardFeePercentage = c.defaultCardFeePercentage,
            taxPercentage = c.defaultTaxPercentage,
            packagingValue = c.defaultPackagingValue,
            deliveryValue = c.defaultDeliveryValue,
            averageItemsPerOrder = c.defaultAverageItemsPerOrder,
            fixedExpensePercentage = c.defaultFixedExpensePercentage,
            profitMarginPercentage = c.defaultProfitMarginPercentage,
        )
        return DefaultPriceFormula.calculate(input)
    }
}
