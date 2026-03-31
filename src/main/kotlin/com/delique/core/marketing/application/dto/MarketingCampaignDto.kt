package com.delique.core.marketing.application.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MarketingCampaignDto(
    val id: Long? = null,
    val name: String,
    val channel: String,
    val investment: BigDecimal,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate,
    val description: String? = null,
    val openRate: BigDecimal? = null,
    val clickRate: BigDecimal? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val createdAt: LocalDateTime? = null,
)
