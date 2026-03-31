package com.delique.core.financial.infrastructure.web

import com.delique.core.financial.application.ExpenseApplicationService
import com.delique.core.financial.application.dto.CreateExpenseDto
import com.delique.core.financial.application.dto.ExpenseDto
import com.delique.core.financial.application.dto.ExpenseEntryDto
import com.delique.core.financial.application.dto.UpdateExpenseDto
import com.delique.core.financial.application.dto.UpcomingExpenseDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/expenses")
@CrossOrigin(origins = ["http://localhost:3000"])
class ExpenseController(
    private val expenseApplicationService: ExpenseApplicationService,
) {
    @GetMapping("/upcoming")
    fun getUpcoming(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<UpcomingExpenseDto>> =
        ResponseEntity.ok(expenseApplicationService.findUpcoming(limit))

    @GetMapping
    fun getByMonth(
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<List<ExpenseDto>> =
        ResponseEntity.ok(expenseApplicationService.findByMonth(year, month))

    @GetMapping("/combined")
    fun getCombined(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<ExpenseEntryDto>> =
        ResponseEntity.ok(expenseApplicationService.findCombined(startDate, endDate))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ExpenseDto> =
        ResponseEntity.ok(expenseApplicationService.getById(id))

    @PostMapping
    fun create(@RequestBody dto: CreateExpenseDto): ResponseEntity<ExpenseDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(expenseApplicationService.create(dto))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: UpdateExpenseDto): ResponseEntity<ExpenseDto> =
        ResponseEntity.ok(expenseApplicationService.update(id, dto))

    @PostMapping("/{id}/pay")
    fun markAsPaid(@PathVariable id: Long): ResponseEntity<ExpenseDto> =
        ResponseEntity.ok(expenseApplicationService.markAsPaid(id))

    @PostMapping("/{id}/unpay")
    fun markAsUnpaid(@PathVariable id: Long): ResponseEntity<ExpenseDto> =
        ResponseEntity.ok(expenseApplicationService.markAsUnpaid(id))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        expenseApplicationService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
