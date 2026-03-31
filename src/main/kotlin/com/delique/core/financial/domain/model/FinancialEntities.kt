package com.delique.core.financial.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "expenses")
class Expense(
    @Column(nullable = false) var name: String,
    @Column(nullable = false, precision = 19, scale = 2) var amount: BigDecimal,
    @Column(name = "due_date", nullable = false) var dueDate: LocalDate,
    @Column(name = "paid_at") var paidAt: LocalDateTime? = null,
    @Column(name = "is_recurring", nullable = false) var isRecurring: Boolean = false,
    @Column(length = 20) var recurrence: String? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

enum class CashPeriodType { WEEK, MONTH, YEAR }

@Entity
@Table(name = "cash")
class Cash(
    @Column(name = "period_type", nullable = false) @Enumerated(EnumType.STRING) var periodType: CashPeriodType = CashPeriodType.MONTH,
    @Column(name = "start_date", nullable = false, unique = true) var startDate: LocalDate,
    @Column(name = "end_date", nullable = false) var endDate: LocalDate,
    @Column(name = "initial_investment", precision = 19, scale = 2, nullable = false) var initialInvestment: BigDecimal = BigDecimal.ZERO,
    @Column(name = "closing_balance", precision = 19, scale = 2) var closingBalance: BigDecimal? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

enum class CashInflowType { INVESTMENT, LOAN, OTHER }

@Entity
@Table(name = "cash_inflows")
class CashInflow(
    @Column(nullable = false, precision = 19, scale = 2) var amount: BigDecimal,
    @Column(nullable = false) var date: LocalDate,
    @Column(length = 500) var description: String? = null,
    @Column(nullable = false, length = 20) @Enumerated(EnumType.STRING) var type: CashInflowType = CashInflowType.OTHER,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
