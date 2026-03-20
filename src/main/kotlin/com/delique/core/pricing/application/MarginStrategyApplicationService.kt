package com.delique.core.pricing.application

import com.delique.core.pricing.domain.model.ABCClass
import com.delique.core.pricing.domain.model.MarginStrategy
import com.delique.core.pricing.domain.model.XYZClass
import com.delique.core.pricing.infrastructure.persistence.MarginStrategyJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MarginStrategyApplicationService(
    private val marginStrategyJpa: MarginStrategyJpa,
) {
    fun getSuggestedMargin(
        abcFaturamento: ABCClass?,
        abcMargem: ABCClass?,
        xyzGiro: XYZClass?,
    ): MarginStrategy? = marginStrategyJpa.findBestMatch(abcFaturamento, abcMargem, xyzGiro).firstOrNull()

    fun findAll(): List<MarginStrategy> = marginStrategyJpa.findAllByOrderBySortOrderAsc()

    @Transactional
    fun createStrategy(strategy: MarginStrategy): MarginStrategy {
        strategy.createdAt = LocalDateTime.now()
        strategy.updatedAt = LocalDateTime.now()
        return marginStrategyJpa.save(strategy)
    }

    @Transactional
    fun updateStrategy(id: Long, strategy: MarginStrategy): MarginStrategy {
        val existing = marginStrategyJpa.findById(id)
            .orElseThrow { IllegalArgumentException("Margin strategy not found with id: $id") }
        existing.suggestedMarginPercentage = strategy.suggestedMarginPercentage
        existing.description = strategy.description
        existing.updatedAt = LocalDateTime.now()
        return marginStrategyJpa.save(existing)
    }

    @Transactional
    fun deleteStrategy(id: Long) {
        marginStrategyJpa.deleteById(id)
    }
}
