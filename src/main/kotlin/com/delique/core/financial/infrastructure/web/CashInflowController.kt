package com.delique.core.financial.infrastructure.web

import com.delique.core.financial.application.CashInflowApplicationService
import com.delique.core.financial.application.dto.CashInflowDto
import com.delique.core.financial.application.dto.CreateCashInflowDto
import com.delique.core.financial.application.dto.InflowEntryDto
import com.delique.core.financial.application.dto.UpdateCashInflowDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/inflows")
class CashInflowController(
    private val cashInflowApplicationService: CashInflowApplicationService,
) {
    @GetMapping
    fun getByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<CashInflowDto>> =
        ResponseEntity.ok(cashInflowApplicationService.findByDateBetween(startDate, endDate))

    @GetMapping("/combined")
    fun getCombined(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<InflowEntryDto>> =
        ResponseEntity.ok(cashInflowApplicationService.findCombined(startDate, endDate))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<CashInflowDto> =
        ResponseEntity.ok(cashInflowApplicationService.getById(id))

    @PostMapping
    fun create(@RequestBody dto: CreateCashInflowDto): ResponseEntity<CashInflowDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(cashInflowApplicationService.create(dto))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: UpdateCashInflowDto,
    ): ResponseEntity<CashInflowDto> =
        ResponseEntity.ok(cashInflowApplicationService.update(id, dto))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        cashInflowApplicationService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
