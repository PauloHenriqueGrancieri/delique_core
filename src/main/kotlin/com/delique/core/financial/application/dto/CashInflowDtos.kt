package com.delique.core.financial.application.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CashInflowDto(
    val id: Long,
    val amount: BigDecimal,
    val date: LocalDate,
    val description: String?,
    val type: String,
)

data class CreateCashInflowDto(
    val amount: BigDecimal,
    val date: LocalDate,
    val description: String? = null,
    val type: String = "OTHER",
)

data class UpdateCashInflowDto(
    val amount: BigDecimal? = null,
    val date: LocalDate? = null,
    val description: String? = null,
    val type: String? = null,
)

/** Combined list entry: manual inflow or sale */
data class InflowEntryDto(
    val type: String,
    val id: Long,
    val date: LocalDate,
    val amount: BigDecimal,
    val description: String? = null,
    val referenceId: Long? = null,
    val manualType: String? = null,
)
