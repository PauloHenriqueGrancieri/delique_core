package com.delique.core.pricing.application.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class PriceCalculationRequest(
    val productId: Long? = null,
    val cmv: BigDecimal? = null,
    val lossPercentage: BigDecimal? = null,
    val salesCommissionPercentage: BigDecimal? = null,
    val cardFeePercentage: BigDecimal? = null,
    val taxPercentage: BigDecimal? = null,
    val packagingValue: BigDecimal? = null,
    val deliveryValue: BigDecimal? = null,
    val averageItemsPerOrder: BigDecimal? = null,
    val fixedExpensePercentage: BigDecimal? = null,
    val profitMarginPercentage: BigDecimal? = null,
)

data class PriceCalculationResponse(
    val productId: Long,
    val productName: String,
    val productDescription: String? = null,
    val cmv: BigDecimal,
    val markup: BigDecimal,
    val calculatedPrice: BigDecimal,
    val finalPrice: BigDecimal? = null,
    val lossPercentage: BigDecimal,
    val salesCommissionPercentage: BigDecimal,
    val cardFeePercentage: BigDecimal,
    val taxPercentage: BigDecimal,
    val packagingValue: BigDecimal,
    val deliveryValue: BigDecimal,
    val averageItemsPerOrder: BigDecimal,
    val custoPedidoUnitario: BigDecimal,
    val percentuais: BigDecimal,
    val fixedExpensePercentage: BigDecimal,
    val profitMarginPercentage: BigDecimal,
)

data class ProductAnalyticsResponse(
    val productId: Long,
    val hasSalesHistory: Boolean,
    val classification: String? = null,
    val abcFaturamento: String? = null,
    val abcMargem: String? = null,
    val xyzGiro: String? = null,
    val suggestedMarginPercentage: BigDecimal? = null,
    val strategyDescription: String? = null,
)

data class PendingPriceCalculationDto(
    val id: Long,
    val productId: Long,
    val productName: String,
    val currentSalePrice: BigDecimal,
    val calculatedPrice: BigDecimal,
    val finalPrice: BigDecimal,
    val cmv: BigDecimal,
    val createdAt: LocalDateTime,
)

data class PaymentMethodDto(
    val id: Long,
    val code: String,
    val name: String,
    val discountPercentage: BigDecimal?,
    val feePercentage: BigDecimal?,
    val installmentFees: List<InstallmentFeeDto>,
)

data class InstallmentFeeDto(val installments: Int, val feePercentage: BigDecimal)

data class PaymentMethodConfigUpdateDto(
    val paymentMethod: String,
    val discountPercentage: BigDecimal?,
    val feePercentage: BigDecimal?,
    val installmentFees: List<InstallmentFeeDto> = emptyList(),
)

data class PaymentMethodConfigBulkUpdateDto(
    val configs: List<PaymentMethodConfigUpdateDto>,
)

data class MarginStrategyDto(
    val id: Long,
    val abcFaturamento: String?,
    val abcMargem: String?,
    val xyzGiro: String?,
    val suggestedMarginPercentage: BigDecimal,
    val description: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class MarginStrategyRequestDto(
    val abcFaturamento: String?,
    val abcMargem: String?,
    val xyzGiro: String?,
    val suggestedMarginPercentage: BigDecimal,
    val description: String?,
    val isActive: Boolean,
)
