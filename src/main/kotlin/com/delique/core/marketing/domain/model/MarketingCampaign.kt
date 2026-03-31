package com.delique.core.marketing.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "marketing_campaign")
class MarketingCampaign(
    @Column(nullable = false) var name: String,
    @Column(nullable = false) var channel: String,
    @Column(nullable = false, precision = 19, scale = 2) var investment: BigDecimal,
    @Column(name = "start_date", nullable = false) var startDate: LocalDate,
    @Column(name = "end_date", nullable = false) var endDate: LocalDate,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Column(name = "open_rate", precision = 5, scale = 2) var openRate: BigDecimal? = null,
    @Column(name = "click_rate", precision = 5, scale = 2) var clickRate: BigDecimal? = null,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
