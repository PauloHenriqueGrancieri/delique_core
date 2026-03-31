package com.delique.core.marketing.application

import com.delique.core.marketing.application.dto.MarketingCampaignDto
import com.delique.core.marketing.domain.model.MarketingCampaign
import com.delique.core.marketing.infrastructure.persistence.MarketingCampaignJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class MarketingCampaignApplicationService(
    private val marketingCampaignJpa: MarketingCampaignJpa,
) {
    fun getAll(): List<MarketingCampaignDto> =
        marketingCampaignJpa.findAll().map { it.toDto() }

    fun getByPeriod(startDate: LocalDate, endDate: LocalDate): List<MarketingCampaignDto> =
        marketingCampaignJpa.findByPeriod(startDate, endDate).map { it.toDto() }

    @Transactional
    fun create(dto: MarketingCampaignDto): MarketingCampaignDto {
        val entity = MarketingCampaign(
            name = dto.name,
            channel = dto.channel,
            investment = dto.investment,
            startDate = dto.startDate,
            endDate = dto.endDate,
            description = dto.description,
            openRate = dto.openRate,
            clickRate = dto.clickRate,
        )
        return marketingCampaignJpa.save(entity).toDto()
    }

    @Transactional
    fun update(id: Long, dto: MarketingCampaignDto): MarketingCampaignDto {
        val entity = marketingCampaignJpa.findById(id)
            .orElseThrow { IllegalArgumentException("Campaign not found: $id") }
        entity.name = dto.name
        entity.channel = dto.channel
        entity.investment = dto.investment
        entity.startDate = dto.startDate
        entity.endDate = dto.endDate
        entity.description = dto.description
        entity.openRate = dto.openRate
        entity.clickRate = dto.clickRate
        return marketingCampaignJpa.save(entity).toDto()
    }

    @Transactional
    fun delete(id: Long) {
        if (!marketingCampaignJpa.existsById(id)) throw IllegalArgumentException("Campaign not found: $id")
        marketingCampaignJpa.deleteById(id)
    }

    private fun MarketingCampaign.toDto() = MarketingCampaignDto(
        id = id,
        name = name,
        channel = channel,
        investment = investment,
        startDate = startDate,
        endDate = endDate,
        description = description,
        openRate = openRate,
        clickRate = clickRate,
        createdAt = createdAt,
    )
}
