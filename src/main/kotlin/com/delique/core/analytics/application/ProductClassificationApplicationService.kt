package com.delique.core.analytics.application

import com.delique.core.analytics.domain.model.PeriodType
import com.delique.core.analytics.domain.model.ProductClassification
import com.delique.core.analytics.infrastructure.persistence.ProductClassificationJpa
import com.delique.core.analytics.infrastructure.persistence.ProductMetricsJpa
import com.delique.core.pricing.domain.model.ABCClass
import com.delique.core.pricing.domain.model.XYZClass
import com.delique.core.product.domain.port.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ProductClassificationApplicationService(
    private val productClassificationJpa: ProductClassificationJpa,
    private val productMetricsJpa: ProductMetricsJpa,
    private val productRepository: ProductRepository,
) {

    fun classifyProductsByRevenue(periodType: PeriodType): Map<Long, ABCClass> {
        val metrics = productMetricsJpa.findByPeriodType(periodType)
            .filter { it.faturamentoTotal > BigDecimal.ZERO }
            .sortedByDescending { it.faturamentoTotal }

        if (metrics.isEmpty()) {
            return emptyMap()
        }

        val totalRevenue = metrics.sumOf { it.faturamentoTotal }
        var accumulatedRevenue = BigDecimal.ZERO
        val classification = mutableMapOf<Long, ABCClass>()

        for (metric in metrics) {
            accumulatedRevenue = accumulatedRevenue.add(metric.faturamentoTotal)
            val percentage = accumulatedRevenue.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))

            val abcClass = when {
                percentage <= BigDecimal("80") -> ABCClass.A
                percentage <= BigDecimal("95") -> ABCClass.B
                else -> ABCClass.C
            }

            classification[metric.product.id] = abcClass
        }

        return classification
    }

    fun classifyProductsByMargin(periodType: PeriodType): Map<Long, ABCClass> {
        val allMetrics = productMetricsJpa.findByPeriodType(periodType)
        val classification = mutableMapOf<Long, ABCClass>()

        allMetrics.filter { it.margemTotal <= BigDecimal.ZERO }
            .forEach { classification[it.product.id] = ABCClass.C }

        val positiveMetrics = allMetrics.filter { it.margemTotal > BigDecimal.ZERO }
            .sortedByDescending { it.margemTotal }

        if (positiveMetrics.isEmpty()) return classification

        val totalMargin = positiveMetrics.sumOf { it.margemTotal }
        var accumulatedMargin = BigDecimal.ZERO

        for (metric in positiveMetrics) {
            accumulatedMargin = accumulatedMargin.add(metric.margemTotal)
            val percentage = accumulatedMargin.divide(totalMargin, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))

            classification[metric.product.id] = when {
                percentage <= BigDecimal("80") -> ABCClass.A
                percentage <= BigDecimal("95") -> ABCClass.B
                else -> ABCClass.C
            }
        }

        return classification
    }

    fun classifyProductsByTurnover(periodType: PeriodType): Map<Long, XYZClass> {
        val metrics = productMetricsJpa.findByPeriodType(periodType)
            .filter { it.quantidadeVendida > 0 }

        if (metrics.isEmpty()) {
            return emptyMap()
        }

        val daysInPeriod = when (periodType) {
            PeriodType.LAST_30_DAYS -> 30
            PeriodType.LAST_60_DAYS -> 60
            PeriodType.LAST_90_DAYS -> 90
            PeriodType.LAST_6_MONTHS -> 180
            PeriodType.LAST_1_YEAR -> 365
        }

        val weeksInPeriod = daysInPeriod / 7.0
        return metrics.associate { metric ->
            val unitsPerWeek = metric.quantidadeVendida.toDouble() / weeksInPeriod
            val xyzClass = when {
                unitsPerWeek >= 1.0 -> XYZClass.X
                unitsPerWeek >= 0.1 -> XYZClass.Y
                else -> XYZClass.Z
            }
            metric.product.id to xyzClass
        }
    }

    @Transactional
    fun classifyAllProducts(periodType: PeriodType): List<ProductClassification> {
        val endDate = LocalDate.now()
        val startDate = when (periodType) {
            PeriodType.LAST_30_DAYS -> endDate.minusDays(30)
            PeriodType.LAST_60_DAYS -> endDate.minusDays(60)
            PeriodType.LAST_90_DAYS -> endDate.minusDays(90)
            PeriodType.LAST_6_MONTHS -> endDate.minusMonths(6)
            PeriodType.LAST_1_YEAR -> endDate.minusYears(1)
        }

        val revenueClassification = classifyProductsByRevenue(periodType)
        val marginClassification = classifyProductsByMargin(periodType)
        val turnoverClassification = classifyProductsByTurnover(periodType)

        val allProductIds = (revenueClassification.keys + marginClassification.keys + turnoverClassification.keys).toSet()
        if (allProductIds.isEmpty()) {
            return emptyList()
        }

        val allProducts = productRepository.findAllById(allProductIds)
        val existingByProductId = productClassificationJpa
            .findByProductInAndPeriodType(allProducts, periodType)
            .associateBy { it.product.id }

        val toSave = allProducts.map { product ->
            val productId = product.id
            val classification = existingByProductId[productId] ?: ProductClassification(
                product = product,
                periodStart = startDate,
                periodEnd = endDate,
                periodType = periodType,
            )

            classification.abcFaturamento = revenueClassification[productId]
            classification.abcMargem = marginClassification[productId]
            classification.xyzGiro = turnoverClassification[productId]
            classification.updatedAt = LocalDateTime.now()

            classification
        }

        return productClassificationJpa.saveAll(toSave)
    }
}
