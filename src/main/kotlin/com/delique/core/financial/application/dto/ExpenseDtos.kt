package com.delique.core.financial.application.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ExpenseDto(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
    val paidAt: LocalDateTime?,
    val isRecurring: Boolean,
    val recurrence: String?,
)

data class CreateExpenseDto(
    val name: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
    val isRecurring: Boolean = false,
    val recurrence: String? = null,
)

data class UpdateExpenseDto(
    val name: String? = null,
    val amount: BigDecimal? = null,
    val dueDate: LocalDate? = null,
    val isRecurring: Boolean? = null,
    val recurrence: String? = null,
)

data class UpcomingExpenseDto(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
)

data class ExpenseEntryDto(
    val type: String,
    val id: Long,
    val date: LocalDate,
    val amount: BigDecimal,
    val name: String,
    val referenceId: Long? = null,
    val paidAt: LocalDateTime? = null,
    val dueDate: LocalDate? = null,
    val isRecurring: Boolean = false,
    val recurrence: String? = null,
)
