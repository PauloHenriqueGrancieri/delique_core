package com.delique.core.pricing.domain.pipeline

import java.math.BigDecimal
import java.math.RoundingMode

data class PricingContext(
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
    val productId: Long?,
    val productName: String,
    val productDescription: String?,
    val cmvAjustado: BigDecimal = BigDecimal.ZERO,
    val custoPedidoUnitario: BigDecimal = BigDecimal.ZERO,
    val markup: BigDecimal = BigDecimal.ZERO,
    val calculatedPrice: BigDecimal = BigDecimal.ZERO,
    val percentuaisSum: BigDecimal = BigDecimal.ZERO,
)

fun interface PriceCalculationStep {
    fun apply(ctx: PricingContext): PricingContext
}

/** Applies the same formula as the legacy monolith in one coherent step (chain endpoint). */
class FullMarkupPriceStep : PriceCalculationStep {
    override fun apply(ctx: PricingContext): PricingContext {
        val lossDecimal = ctx.lossPercentage.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val cmvAjustado = ctx.cmv.divide(BigDecimal.ONE.subtract(lossDecimal), 2, RoundingMode.HALF_UP)
        val custoPedidoUnitario = ctx.packagingValue
            .add(ctx.deliveryValue)
            .divide(ctx.averageItemsPerOrder, 4, RoundingMode.HALF_UP)
        val percentuais = ctx.salesCommissionPercentage
            .add(ctx.cardFeePercentage)
            .add(ctx.taxPercentage)
            .add(ctx.fixedExpensePercentage)
            .add(ctx.profitMarginPercentage)
        val percentuaisDecimal = percentuais.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val denominator = BigDecimal.ONE.subtract(percentuaisDecimal)
        require(denominator > BigDecimal.ZERO) { "A soma dos percentuais não pode ser maior ou igual a 100%" }
        val markup = BigDecimal.ONE.divide(denominator, 4, RoundingMode.HALF_UP)
        val calculatedPrice = cmvAjustado
            .add(custoPedidoUnitario)
            .multiply(markup)
            .setScale(2, RoundingMode.HALF_UP)
        return ctx.copy(
            cmvAjustado = cmvAjustado,
            custoPedidoUnitario = custoPedidoUnitario.setScale(2, RoundingMode.HALF_UP),
            markup = markup,
            calculatedPrice = calculatedPrice,
            percentuaisSum = percentuais.setScale(2, RoundingMode.HALF_UP),
        )
    }
}

fun interface PricingStrategy {
    fun calculate(ctx: PricingContext): PricingContext
}

class DefaultPricingStrategy(
    private val steps: List<PriceCalculationStep>,
) : PricingStrategy {
    override fun calculate(ctx: PricingContext): PricingContext =
        steps.fold(ctx) { acc, step -> step.apply(acc) }
}
