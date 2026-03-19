package com.delique.core.analytics.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BusinessAnalyticsJob(
    private val businessAnalyticsApplicationService: BusinessAnalyticsApplicationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Runs at 02:00 on the 1st day of each month. */
    @Scheduled(cron = "0 0 2 1 * ?")
    fun recalculateAllMetrics() {
        try {
            businessAnalyticsApplicationService.recalculateAllMetrics()
        } catch (e: Exception) {
            logger.error("Error during monthly business analytics recalculation", e)
        }
    }
}
