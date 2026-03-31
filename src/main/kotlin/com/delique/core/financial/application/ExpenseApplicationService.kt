package com.delique.core.financial.application

import com.delique.core.financial.application.dto.*
import com.delique.core.financial.domain.model.Expense
import com.delique.core.financial.infrastructure.persistence.ExpenseJpa
import com.delique.core.inventory.infrastructure.persistence.JpaPurchaseOrderJpa
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class ExpenseApplicationService(
    private val expenseJpa: ExpenseJpa,
    private val purchaseOrderJpa: JpaPurchaseOrderJpa,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getById(id: Long): ExpenseDto {
        val e = expenseJpa.findById(id).orElseThrow {
            log.warn("Expense not found: id=$id")
            IllegalArgumentException("Expense not found: $id")
        }
        return e.toDto()
    }

    fun findByMonth(year: Int, month: Int): List<ExpenseDto> {
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        return expenseJpa.findByDueDateBetweenOrderByDueDateAsc(start, end).map { it.toDto() }
    }

    fun findUpcoming(limit: Int = 20): List<UpcomingExpenseDto> {
        val today = LocalDate.now()
        val list = expenseJpa.findByDueDateGreaterThanEqualAndPaidAtIsNullOrderByDueDateAsc(today, PageRequest.of(0, limit))
        return list.map { UpcomingExpenseDto(it.id, it.name, it.amount, it.dueDate) }
    }

    fun findCombined(startDate: LocalDate, endDate: LocalDate): List<ExpenseEntryDto> {
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)
        val expensesInRange = expenseJpa.findByDueDateBetweenOrderByDueDateAsc(startDate, endDate)
        val expenses = expensesInRange.map {
            ExpenseEntryDto(
                type = "EXPENSE",
                id = it.id,
                date = it.paidAt?.toLocalDate() ?: it.dueDate,
                amount = it.amount,
                name = it.name,
                referenceId = null,
                paidAt = it.paidAt,
                dueDate = it.dueDate,
                isRecurring = it.isRecurring,
                recurrence = it.recurrence,
            )
        }
        val pos = purchaseOrderJpa.findDeliveredBetween(start, end)
        val poEntries = pos.map { po ->
            val total = po.items.fold(po.totalFreight) { acc, i ->
                acc.add(i.unitCost.multiply(BigDecimal(i.quantity)))
            }
            ExpenseEntryDto(
                type = "PURCHASE_ORDER",
                id = po.id,
                date = po.deliveredAt!!.toLocalDate(),
                amount = total,
                name = "Pedido de compra #${po.id}",
                referenceId = po.id,
            )
        }
        return (expenses + poEntries).sortedBy { it.date }
    }

    @Transactional
    fun create(dto: CreateExpenseDto): ExpenseDto {
        val e = Expense(
            name = dto.name,
            amount = dto.amount,
            dueDate = dto.dueDate,
            isRecurring = dto.isRecurring,
            recurrence = dto.recurrence,
        )
        return expenseJpa.save(e).toDto()
    }

    @Transactional
    fun update(id: Long, dto: UpdateExpenseDto): ExpenseDto {
        val e = expenseJpa.findById(id).orElseThrow { IllegalArgumentException("Expense not found: $id") }
        dto.name?.let { e.name = it }
        dto.amount?.let { e.amount = it }
        dto.dueDate?.let { e.dueDate = it }
        dto.isRecurring?.let { e.isRecurring = it }
        dto.recurrence?.let { e.recurrence = it }
        return expenseJpa.save(e).toDto()
    }

    @Transactional
    fun markAsPaid(id: Long): ExpenseDto {
        val e = expenseJpa.findById(id).orElseThrow { IllegalArgumentException("Expense not found: $id") }
        e.paidAt = LocalDateTime.now()
        return expenseJpa.save(e).toDto()
    }

    @Transactional
    fun markAsUnpaid(id: Long): ExpenseDto {
        val e = expenseJpa.findById(id).orElseThrow { IllegalArgumentException("Expense not found: $id") }
        e.paidAt = null
        return expenseJpa.save(e).toDto()
    }

    @Transactional
    fun delete(id: Long) {
        if (!expenseJpa.existsById(id)) throw IllegalArgumentException("Expense not found: $id")
        expenseJpa.deleteById(id)
    }

    private fun Expense.toDto() = ExpenseDto(
        id = id,
        name = name,
        amount = amount,
        dueDate = dueDate,
        paidAt = paidAt,
        isRecurring = isRecurring,
        recurrence = recurrence,
    )
}
