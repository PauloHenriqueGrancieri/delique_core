package com.delique.core.financial.application.dto

import java.math.BigDecimal
import java.time.LocalDate

data class OverviewMetricsDto(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val initialInvestment: BigDecimal,
    val openingBalance: BigDecimal,
    val salesTotal: BigDecimal,
    val purchasesTotal: BigDecimal,
    val expensesTotal: BigDecimal,
    val inflowsTotal: BigDecimal,
    val cancelledOrdersRefundTotal: BigDecimal,
    val cashBalance: BigDecimal,
    val orderCount: Long,
    val itemsSold: Long,
    val avgItemsPerOrder: BigDecimal,
    val paymentMethodShares: Map<String, BigDecimal>,
    val topProducts: List<TopProductDto>,
    val topCategories: List<TopCategoryDto>,
)

data class TopProductDto(
    val productId: Long,
    val productName: String,
    val quantity: Long,
    val totalValue: BigDecimal,
)

data class TopCategoryDto(
    val categoryName: String,
    val quantity: Long,
    val totalValue: BigDecimal,
)

data class UpdateCashDto(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val initialInvestment: BigDecimal,
)
