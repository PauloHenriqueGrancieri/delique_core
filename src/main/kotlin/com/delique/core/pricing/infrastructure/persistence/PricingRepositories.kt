package com.delique.core.pricing.infrastructure.persistence

import com.delique.core.pricing.domain.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
interface PriceCalculationConfigJpa : JpaRepository<PriceCalculationConfig, Long> {
    fun findFirstByOrderByIdAsc(): PriceCalculationConfig?
}

interface PaymentMethodConfigJpa : JpaRepository<PaymentMethodConfig, Long> {
    fun findByPaymentMethod(method: String): PaymentMethodConfig?
}

interface CreditCardInstallmentFeeJpa : JpaRepository<CreditCardInstallmentFee, Long> {
    fun findByInstallments(n: Int): CreditCardInstallmentFee?
    fun findAllByOrderByInstallmentsAsc(): List<CreditCardInstallmentFee>
}

interface MarginStrategyJpa : JpaRepository<MarginStrategy, Long> {
    fun findAllByOrderBySortOrderAsc(): List<MarginStrategy>

    @Query(
        """
        SELECT ms FROM MarginStrategy ms 
        WHERE ms.isActive = true 
        AND (ms.abcFaturamento = :abcFaturamento OR ms.abcFaturamento IS NULL)
        AND (ms.abcMargem = :abcMargem OR ms.abcMargem IS NULL)
        AND (ms.xyzGiro = :xyzGiro OR ms.xyzGiro IS NULL)
        ORDER BY
            (CASE WHEN ms.abcFaturamento IS NOT NULL THEN 1 ELSE 0 END +
             CASE WHEN ms.abcMargem IS NOT NULL THEN 1 ELSE 0 END +
             CASE WHEN ms.xyzGiro IS NOT NULL THEN 1 ELSE 0 END) DESC
        """,
    )
    fun findBestMatch(
        abcFaturamento: ABCClass?,
        abcMargem: ABCClass?,
        xyzGiro: XYZClass?,
    ): List<MarginStrategy>
}

interface PendingPriceCalculationJpa : JpaRepository<PendingPriceCalculation, Long> {
    fun findByProduct_Id(productId: Long): PendingPriceCalculation?
    fun findAllByOrderByCreatedAtDesc(): List<PendingPriceCalculation>
}
