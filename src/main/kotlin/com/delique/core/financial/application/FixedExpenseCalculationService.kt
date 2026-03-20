package com.delique.core.financial.application

import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import com.delique.core.financial.infrastructure.persistence.ExpenseJpa
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Service
class FixedExpenseCalculationService(
    private val expenseJpa: ExpenseJpa,
    private val customerOrderJpa: CustomerOrderJpa,
) {
    fun calculateFixedExpensePercentageOverRevenue(months: Int = 3): BigDecimal {
        val monthlyFixedExpense = calculateMonthlyFixedExpense()
        val avgMonthlySales = calculateAverageMonthlySales(months)
        if (avgMonthlySales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO
        }
        return monthlyFixedExpense
            .multiply(BigDecimal("100"))
            .divide(avgMonthlySales, 2, RoundingMode.HALF_UP)
    }

    fun calculateMonthlyFixedExpense(): BigDecimal {
        val allExpenses = expenseJpa.findAll()
        var recurringMonthly = BigDecimal.ZERO
        for (e in allExpenses) {
            if (e.isRecurring) {
                val monthly = if ("YEARLY".equals(e.recurrence, ignoreCase = true)) {
                    e.amount.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)
                } else {
                    e.amount
                }
                recurringMonthly = recurringMonthly.add(monthly)
            }
        }
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(1)
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)
        val paidInPeriod = expenseJpa.findByPaidAtBetweenOrderByPaidAtAsc(start, end)
        val nonRecurringSum = paidInPeriod
            .filter { !it.isRecurring }
            .fold(BigDecimal.ZERO) { acc, ex -> acc.add(ex.amount) }
        val nonRecurringMonthly = nonRecurringSum.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)
        return recurringMonthly.add(nonRecurringMonthly)
    }

    fun calculateAverageMonthlySales(months: Int = 3): BigDecimal {
        val endDate = LocalDate.now()
        val firstOrderDate = customerOrderJpa.findFirstOrderDate()?.toLocalDate()
        val effectiveMonths = if (firstOrderDate != null) {
            val monthsSinceFirst = ChronoUnit.MONTHS.between(firstOrderDate, endDate).toInt() + 1
            minOf(months, maxOf(1, monthsSinceFirst))
        } else {
            months
        }
        val startDate = endDate.minusMonths(effectiveMonths.toLong())
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)
        val orders = customerOrderJpa.findByCreatedAtBetweenWithSales(start, end)
        var salesTotal = BigDecimal.ZERO
        for (order in orders) {
            var orderSubtotal = BigDecimal.ZERO
            for (sale in order.sales) {
                val isBrinde = sale.product.category.name.equals("Brinde", ignoreCase = true)
                if (!isBrinde) {
                    orderSubtotal = orderSubtotal.add(
                        sale.unitPrice.multiply(BigDecimal(sale.quantity)).subtract(sale.discount ?: BigDecimal.ZERO),
                    )
                }
            }
            val totalWithDiscount = orderSubtotal.subtract(order.orderDiscountValue ?: BigDecimal.ZERO)
            val orderEntry = totalWithDiscount.subtract(order.feeValue ?: BigDecimal.ZERO)
            salesTotal = salesTotal.add(orderEntry)
        }
        return salesTotal.divide(BigDecimal(effectiveMonths), 2, RoundingMode.HALF_UP)
    }
}
