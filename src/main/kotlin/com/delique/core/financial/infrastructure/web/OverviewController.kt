package com.delique.core.financial.infrastructure.web

import com.delique.core.financial.application.OverviewApplicationService
import com.delique.core.financial.application.dto.OverviewMetricsDto
import com.delique.core.financial.application.dto.UpdateCashDto
import com.delique.core.financial.domain.model.CashPeriodType
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/overview")
@CrossOrigin(origins = ["http://localhost:3000"])
class OverviewController(
    private val overviewApplicationService: OverviewApplicationService,
) {
    @GetMapping("/metrics")
    fun getMetrics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(defaultValue = "MONTH") periodType: String,
    ): ResponseEntity<OverviewMetricsDto> {
        val type = try {
            CashPeriodType.valueOf(periodType.uppercase())
        } catch (_: Exception) {
            CashPeriodType.MONTH
        }
        return ResponseEntity.ok(overviewApplicationService.getMetrics(startDate, endDate, type))
    }

    @PutMapping("/cash")
    fun updateCash(
        @RequestBody dto: UpdateCashDto,
        @RequestParam(defaultValue = "MONTH") periodType: String,
    ): ResponseEntity<OverviewMetricsDto> {
        val type = try {
            CashPeriodType.valueOf(periodType.uppercase())
        } catch (_: Exception) {
            CashPeriodType.MONTH
        }
        return ResponseEntity.ok(overviewApplicationService.updateInitialInvestment(dto, type))
    }
}
