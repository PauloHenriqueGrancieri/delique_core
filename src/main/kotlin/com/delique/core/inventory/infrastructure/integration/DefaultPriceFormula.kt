package com.delique.core.inventory.infrastructure.integration

import java.math.BigDecimal
import java.math.RoundingMode

data class PriceCalculationInput(
    val productId: Long?,
    val cmv: BigDecimal,
    val lossPercentage: BigDecimal,
    val salesCommissionPercentage: BigDecimal,
    val cardFeePercentage: BigDecimal,
    val taxPercentage: BigDecimal,
    val packagingValue: BigDecimal,
    val deliveryValue: BigDecimal,
    val averageItemsPerOrder: BigDecimal,
    val fixedExpensePercentage: BigDecimal,
    val profitMarginPercentage: BigDecimal,
)

data class PriceCalculationResult(
    val calculatedPrice: BigDecimal,
)

object DefaultPriceFormula {
    fun calculate(request: PriceCalculationInput): PriceCalculationResult {
        val lossDecimal = request.lossPercentage.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val cmvAjustado = request.cmv.divide(BigDecimal.ONE.subtract(lossDecimal), 2, RoundingMode.HALF_UP)
        val custoPedidoUnitario = request.packagingValue
            .add(request.deliveryValue)
            .divide(request.averageItemsPerOrder, 4, RoundingMode.HALF_UP)
        val percentuais = request.salesCommissionPercentage
            .add(request.cardFeePercentage)
            .add(request.taxPercentage)
            .add(request.fixedExpensePercentage)
            .add(request.profitMarginPercentage)
        val percentuaisDecimal = percentuais.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val denominator = BigDecimal.ONE.subtract(percentuaisDecimal)
        require(denominator > BigDecimal.ZERO) { "Percent sum must be below 100%" }
        val markup = BigDecimal.ONE.divide(denominator, 4, RoundingMode.HALF_UP)
        val calculatedPrice = cmvAjustado
            .add(custoPedidoUnitario)
            .multiply(markup)
            .setScale(2, RoundingMode.HALF_UP)
        return PriceCalculationResult(calculatedPrice = calculatedPrice)
    }
}
