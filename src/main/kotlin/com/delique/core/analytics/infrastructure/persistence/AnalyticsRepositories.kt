package com.delique.core.analytics.infrastructure.persistence

import com.delique.core.analytics.domain.model.PeriodType
import com.delique.core.analytics.domain.model.ProductClassification
import com.delique.core.analytics.domain.model.ProductMetrics
import com.delique.core.product.domain.model.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductMetricsJpa : JpaRepository<ProductMetrics, Long> {
    fun findByProductAndPeriodType(product: Product, periodType: PeriodType): ProductMetrics?
    fun findByPeriodType(periodType: PeriodType): List<ProductMetrics>
}

interface ProductClassificationJpa : JpaRepository<ProductClassification, Long> {
    fun findByProductAndPeriodType(product: Product, periodType: PeriodType): ProductClassification?
    fun findByProductInAndPeriodType(products: List<Product>, periodType: PeriodType): List<ProductClassification>
}
