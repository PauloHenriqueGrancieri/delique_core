package com.delique.core.marketing.infrastructure.web

import com.delique.core.marketing.application.MarketingCampaignApplicationService
import com.delique.core.marketing.application.dto.MarketingCampaignDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/marketing-campaigns")
@CrossOrigin(origins = ["http://localhost:3000"])
class MarketingCampaignController(
    private val marketingCampaignApplicationService: MarketingCampaignApplicationService,
) {
    @GetMapping
    fun getAll(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    ): ResponseEntity<List<MarketingCampaignDto>> {
        val result = if (startDate != null && endDate != null) {
            marketingCampaignApplicationService.getByPeriod(startDate, endDate)
        } else {
            marketingCampaignApplicationService.getAll()
        }
        return ResponseEntity.ok(result)
    }

    @PostMapping
    fun create(@RequestBody dto: MarketingCampaignDto): ResponseEntity<MarketingCampaignDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(marketingCampaignApplicationService.create(dto))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: MarketingCampaignDto): ResponseEntity<MarketingCampaignDto> =
        ResponseEntity.ok(marketingCampaignApplicationService.update(id, dto))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        marketingCampaignApplicationService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
