package com.delique.core.financial.infrastructure.persistence

import com.delique.core.financial.domain.model.Cash
import com.delique.core.financial.domain.model.CashInflow
import com.delique.core.financial.domain.model.CashPeriodType
import com.delique.core.financial.domain.model.Expense
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.time.LocalDateTime

interface ExpenseJpa : JpaRepository<Expense, Long> {
    fun findByPaidAtBetweenOrderByPaidAtAsc(start: LocalDateTime, end: LocalDateTime): List<Expense>

    fun findByDueDateBetweenOrderByDueDateAsc(start: LocalDate, end: LocalDate): List<Expense>

    fun findByDueDateGreaterThanEqualAndPaidAtIsNullOrderByDueDateAsc(today: LocalDate, pageable: Pageable): List<Expense>
}

interface CashJpa : JpaRepository<Cash, Long> {
    fun findByStartDate(date: LocalDate): Cash?
    fun findTop1ByPeriodTypeAndStartDateBeforeOrderByStartDateDesc(type: CashPeriodType, date: LocalDate): Cash?
}

interface CashInflowJpa : JpaRepository<CashInflow, Long> {
    fun findByDateBetweenOrderByDateAsc(start: LocalDate, end: LocalDate): List<CashInflow>
}
