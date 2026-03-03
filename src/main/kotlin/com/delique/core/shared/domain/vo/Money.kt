package com.delique.core.shared.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.math.RoundingMode

@Embeddable
data class Money(
    @Column(precision = 19, scale = 2)
    val amount: BigDecimal = BigDecimal.ZERO,
) : Comparable<Money> {

    companion object {
        val ZERO = Money(BigDecimal.ZERO)
        fun of(value: Double)  = Money(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP))
        fun of(value: String)  = Money(BigDecimal(value).setScale(2, RoundingMode.HALF_UP))
        fun of(value: Long)    = Money(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP))
    }

    operator fun plus(other: Money)         = Money((amount + other.amount).setScale(2, RoundingMode.HALF_UP))
    operator fun minus(other: Money)        = Money((amount - other.amount).setScale(2, RoundingMode.HALF_UP))
    operator fun times(factor: BigDecimal)  = Money((amount * factor).setScale(2, RoundingMode.HALF_UP))
    operator fun times(factor: Double)      = this * BigDecimal.valueOf(factor)
    operator fun div(divisor: BigDecimal)   = Money((amount / divisor).setScale(2, RoundingMode.HALF_UP))

    fun percentageOf(percent: BigDecimal)   = Money((amount * percent / BigDecimal(100)).setScale(2, RoundingMode.HALF_UP))
    fun applyMarkup(percent: BigDecimal)    = Money((amount * (BigDecimal.ONE + percent / BigDecimal(100))).setScale(2, RoundingMode.HALF_UP))
    fun applyDiscount(percent: BigDecimal)  = Money((amount * (BigDecimal.ONE - percent / BigDecimal(100))).setScale(2, RoundingMode.HALF_UP))

    fun isZero()     = amount.compareTo(BigDecimal.ZERO) == 0
    fun isPositive() = amount > BigDecimal.ZERO
    fun isNegative() = amount < BigDecimal.ZERO

    override fun compareTo(other: Money) = amount.compareTo(other.amount)
    override fun toString() = "R$ ${amount.setScale(2, RoundingMode.HALF_UP)}"
}
