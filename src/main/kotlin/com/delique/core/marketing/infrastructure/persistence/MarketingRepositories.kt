package com.delique.core.marketing.infrastructure.persistence

import com.delique.core.marketing.domain.model.MarketingCampaign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface MarketingCampaignJpa : JpaRepository<MarketingCampaign, Long> {
    @Query("SELECT c FROM MarketingCampaign c WHERE c.startDate <= :end AND c.endDate >= :start")
    fun findByPeriod(@Param("start") start: LocalDate, @Param("end") end: LocalDate): List<MarketingCampaign>
}
