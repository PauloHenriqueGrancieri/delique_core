package com.delique.core.financial.application

import com.delique.core.financial.application.dto.*
import com.delique.core.financial.domain.model.Cash
import com.delique.core.financial.domain.model.CashPeriodType
import com.delique.core.financial.infrastructure.persistence.CashInflowJpa
import com.delique.core.financial.infrastructure.persistence.CashJpa
import com.delique.core.financial.infrastructure.persistence.ExpenseJpa
import com.delique.core.inventory.infrastructure.persistence.JpaPurchaseOrderJpa
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@Service
class OverviewApplicationService(
    private val customerOrderJpa: CustomerOrderJpa,
    private val purchaseOrderJpa: JpaPurchaseOrderJpa,
    private val cashJpa: CashJpa,
    private val expenseJpa: ExpenseJpa,
    private val cashInflowJpa: CashInflowJpa,
) {
    private fun canonicalEndDate(periodType: CashPeriodType, startDate: LocalDate): LocalDate = when (periodType) {
        CashPeriodType.WEEK -> startDate.plusDays(6)
        CashPeriodType.MONTH -> startDate.with(TemporalAdjusters.lastDayOfMonth())
        CashPeriodType.YEAR -> startDate.with(TemporalAdjusters.lastDayOfYear())
    }

    private fun computeClosingBalanceForPeriod(periodStartDate: LocalDate, periodType: CashPeriodType): BigDecimal {
        val periodEndDate = canonicalEndDate(periodType, periodStartDate)
        val prev = cashJpa.findTop1ByPeriodTypeAndStartDateBeforeOrderByStartDateDesc(periodType, periodStartDate)
        val opening = when {
            prev == null -> cashJpa.findByStartDate(periodStartDate)?.initialInvestment ?: BigDecimal.ZERO
            prev.closingBalance != null -> prev.closingBalance!!
            else -> computeClosingBalanceForPeriod(prev.startDate, periodType)
        }
        val start = periodStartDate.atStartOfDay()
        val end = periodEndDate.atTime(LocalTime.MAX)
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
        val deliveredPurchases = purchaseOrderJpa.findDeliveredBetween(start, end)
        val cancelledPurchases = purchaseOrderJpa.findCancelledBetween(start, end)
        var purchasesTotal = BigDecimal.ZERO
        for (po in deliveredPurchases) {
            purchasesTotal = purchasesTotal.add(
                po.items.fold(po.totalFreight) { acc, i ->
                    acc.add(i.unitCost.multiply(BigDecimal(i.quantity)))
                },
            )
        }
        var cancelledRefundTotal = BigDecimal.ZERO
        for (po in cancelledPurchases) {
            cancelledRefundTotal = cancelledRefundTotal.add(
                po.items.fold(po.totalFreight) { acc, i ->
                    acc.add(i.unitCost.multiply(BigDecimal(i.quantity)))
                },
            )
        }
        val expensesPaid = expenseJpa.findByPaidAtBetweenOrderByPaidAtAsc(start, end)
        val expensesTotal = expensesPaid.fold(BigDecimal.ZERO) { acc, ex -> acc.add(ex.amount) }
        val inflows = cashInflowJpa.findByDateBetweenOrderByDateAsc(periodStartDate, periodEndDate)
        val inflowsTotal = inflows.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.amount) }
        return opening.add(salesTotal).add(inflowsTotal).add(cancelledRefundTotal).subtract(purchasesTotal).subtract(expensesTotal)
    }

    fun getMetrics(startDate: LocalDate, endDate: LocalDate, periodType: CashPeriodType): OverviewMetricsDto {
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)
        val orders = customerOrderJpa.findByCreatedAtBetweenWithSales(start, end)
        val deliveredPurchases = purchaseOrderJpa.findDeliveredBetween(start, end)
        val cancelledPurchases = purchaseOrderJpa.findCancelledBetween(start, end)

        var salesTotal = BigDecimal.ZERO
        var totalWithDiscountSum = BigDecimal.ZERO
        var orderCount = 0L
        var itemsSold = 0L
        val paymentTotals = mutableMapOf<String, BigDecimal>()
        val productQuantities = mutableMapOf<Long, Pair<Long, BigDecimal>>()
        val productNames = mutableMapOf<Long, String>()
        val categoryQuantities = mutableMapOf<String, Pair<Long, BigDecimal>>()

        for (order in orders) {
            orderCount++
            var orderSubtotal = BigDecimal.ZERO
            for (sale in order.sales) {
                val isBrinde = sale.product.category.name.equals("Brinde", ignoreCase = true)
                val itemTotal = sale.unitPrice.multiply(BigDecimal(sale.quantity))
                    .subtract(sale.discount ?: BigDecimal.ZERO)
                if (!isBrinde) {
                    orderSubtotal = orderSubtotal.add(itemTotal)
                }
                itemsSold += sale.quantity
                if (!isBrinde) {
                    val key = sale.product.id
                    val (q, v) = productQuantities.getOrPut(key) { 0L to BigDecimal.ZERO }
                    productQuantities[key] = (q + sale.quantity) to (v.add(itemTotal))
                    productNames[key] = sale.product.name
                    val cat = sale.product.category.name
                    val (cq, cv) = categoryQuantities.getOrPut(cat) { 0L to BigDecimal.ZERO }
                    categoryQuantities[cat] = (cq + sale.quantity) to (cv.add(itemTotal))
                }
            }
            val totalWithDiscount = orderSubtotal.subtract(order.orderDiscountValue ?: BigDecimal.ZERO)
            val orderEntry = totalWithDiscount.subtract(order.feeValue ?: BigDecimal.ZERO)
            salesTotal = salesTotal.add(orderEntry)
            totalWithDiscountSum = totalWithDiscountSum.add(totalWithDiscount)
            val pm = order.paymentMethod
            paymentTotals[pm] = (paymentTotals[pm] ?: BigDecimal.ZERO).add(totalWithDiscount)
        }

        var purchasesTotal = BigDecimal.ZERO
        for (po in deliveredPurchases) {
            val poTotal = po.items.fold(po.totalFreight) { acc, i ->
                acc.add(i.unitCost.multiply(BigDecimal(i.quantity)))
            }
            purchasesTotal = purchasesTotal.add(poTotal)
        }
        var cancelledRefundTotal = BigDecimal.ZERO
        for (po in cancelledPurchases) {
            val poTotal = po.items.fold(po.totalFreight) { acc, i ->
                acc.add(i.unitCost.multiply(BigDecimal(i.quantity)))
            }
            cancelledRefundTotal = cancelledRefundTotal.add(poTotal)
        }

        val cash = cashJpa.findByStartDate(startDate)
            ?: cashJpa.save(Cash(periodType = periodType, startDate = startDate, endDate = endDate))
        val previousCash = cashJpa.findTop1ByPeriodTypeAndStartDateBeforeOrderByStartDateDesc(periodType, startDate)
        val openingBalance = when {
            previousCash == null -> cash.initialInvestment
            previousCash.closingBalance != null -> previousCash.closingBalance!!
            else -> computeClosingBalanceForPeriod(previousCash.startDate, periodType)
        }
        val expensesPaid = expenseJpa.findByPaidAtBetweenOrderByPaidAtAsc(start, end)
        val expensesTotal = expensesPaid.fold(BigDecimal.ZERO) { acc, ex -> acc.add(ex.amount) }
        val inflows = cashInflowJpa.findByDateBetweenOrderByDateAsc(startDate, endDate)
        val inflowsTotal = inflows.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.amount) }
        val closingBalance = openingBalance.add(salesTotal).add(inflowsTotal).add(cancelledRefundTotal)
            .subtract(purchasesTotal).subtract(expensesTotal)
        val isFullPeriod = endDate == canonicalEndDate(periodType, startDate)
        if (isFullPeriod) {
            cash.closingBalance = closingBalance
            cash.endDate = endDate
            cashJpa.save(cash)
        }

        val paymentMethodShares = if (totalWithDiscountSum.compareTo(BigDecimal.ZERO) == 0) {
            paymentTotals.keys.associateWith { BigDecimal.ZERO }
        } else {
            paymentTotals.mapValues { (_, v) ->
                v.multiply(BigDecimal(100)).divide(totalWithDiscountSum, 2, RoundingMode.HALF_UP)
            }
        }

        val avgItems = if (orderCount == 0L) BigDecimal.ZERO
        else BigDecimal(itemsSold).divide(BigDecimal(orderCount), 2, RoundingMode.HALF_UP)

        val topProducts = productQuantities.entries
            .map { (id, p) -> TopProductDto(id, productNames[id] ?: "", p.first, p.second) }
            .sortedByDescending { it.quantity }
            .take(10)

        val topCategories = categoryQuantities.entries
            .map { (name, p) -> TopCategoryDto(name, p.first, p.second) }
            .sortedByDescending { it.quantity }
            .take(10)

        return OverviewMetricsDto(
            startDate = startDate,
            endDate = endDate,
            initialInvestment = cash.initialInvestment,
            openingBalance = openingBalance,
            salesTotal = salesTotal,
            purchasesTotal = purchasesTotal,
            expensesTotal = expensesTotal,
            inflowsTotal = inflowsTotal,
            cancelledOrdersRefundTotal = cancelledRefundTotal,
            cashBalance = closingBalance,
            orderCount = orderCount,
            itemsSold = itemsSold,
            avgItemsPerOrder = avgItems,
            paymentMethodShares = paymentMethodShares,
            topProducts = topProducts,
            topCategories = topCategories,
        )
    }

    @Transactional
    fun updateInitialInvestment(dto: UpdateCashDto, periodType: CashPeriodType): OverviewMetricsDto {
        var cash = cashJpa.findByStartDate(dto.startDate)
        if (cash == null) {
            cash = cashJpa.save(
                Cash(
                    periodType = periodType,
                    startDate = dto.startDate,
                    endDate = dto.endDate,
                    initialInvestment = dto.initialInvestment,
                ),
            )
        } else {
            cash.initialInvestment = dto.initialInvestment
            cash.endDate = dto.endDate
            cashJpa.save(cash)
        }
        return getMetrics(dto.startDate, dto.endDate, periodType)
    }
}
