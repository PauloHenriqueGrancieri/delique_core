package com.delique.core.analytics.application

import com.delique.core.analytics.domain.model.PeriodType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BusinessAnalyticsApplicationService(
    private val productMetricsApplicationService: ProductMetricsApplicationService,
    private val productClassificationApplicationService: ProductClassificationApplicationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun recalculateAllMetrics() {
        logger.info("Starting business analytics recalculation...")
        val periodTypes = listOf(
            PeriodType.LAST_30_DAYS,
            PeriodType.LAST_60_DAYS,
            PeriodType.LAST_90_DAYS,
            PeriodType.LAST_6_MONTHS,
            PeriodType.LAST_1_YEAR,
        )
        for (periodType in periodTypes) {
            logger.info("Calculating metrics for period: $periodType")
            productMetricsApplicationService.calculateMetricsForAllProducts(periodType)
        }
        logger.info("Calculating classifications for period: LAST_90_DAYS")
        productClassificationApplicationService.classifyAllProducts(PeriodType.LAST_90_DAYS)
        logger.info("Business analytics recalculation completed successfully")
    }
}
