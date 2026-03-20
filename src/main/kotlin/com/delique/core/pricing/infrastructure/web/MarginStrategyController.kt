package com.delique.core.pricing.infrastructure.web

import com.delique.core.pricing.application.dto.MarginStrategyDto
import com.delique.core.pricing.application.dto.MarginStrategyRequestDto
import com.delique.core.pricing.application.MarginStrategyApplicationService
import com.delique.core.pricing.domain.model.ABCClass
import com.delique.core.pricing.domain.model.MarginStrategy
import com.delique.core.pricing.domain.model.XYZClass
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/margin-strategies")
@CrossOrigin(origins = ["http://localhost:3000"])
class MarginStrategyController(
    private val marginStrategyApplicationService: MarginStrategyApplicationService,
) {
    @GetMapping
    fun getAll(): ResponseEntity<List<MarginStrategyDto>> =
        ResponseEntity.ok(marginStrategyApplicationService.findAll().map { it.toDto() })

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: MarginStrategyRequestDto,
    ): ResponseEntity<MarginStrategyDto> {
        val updated = marginStrategyApplicationService.updateStrategy(id, dto.toEntity())
        return ResponseEntity.ok(updated.toDto())
    }

    private fun MarginStrategy.toDto() = MarginStrategyDto(
        id = id,
        abcFaturamento = abcFaturamento?.name,
        abcMargem = abcMargem?.name,
        xyzGiro = xyzGiro?.name,
        suggestedMarginPercentage = suggestedMarginPercentage,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun MarginStrategyRequestDto.toEntity(): MarginStrategy {
        val abcF = abcFaturamento?.trim()?.takeIf { it.isNotEmpty() }?.let { ABCClass.valueOf(it) }
        val abcM = abcMargem?.trim()?.takeIf { it.isNotEmpty() }?.let { ABCClass.valueOf(it) }
        val xyz = xyzGiro?.trim()?.takeIf { it.isNotEmpty() }?.let { XYZClass.valueOf(it) }
        return MarginStrategy(
            abcFaturamento = abcF,
            abcMargem = abcM,
            xyzGiro = xyz,
            suggestedMarginPercentage = suggestedMarginPercentage,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive,
        )
    }
}
