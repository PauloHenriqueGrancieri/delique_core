package com.delique.core.pricing.domain.model

import com.delique.core.product.domain.model.Product
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "price_calculation_config")
class PriceCalculationConfig(
    @Column(name = "default_cmv", precision = 19, scale = 2)
    var defaultCmv: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_loss_percentage", precision = 5, scale = 2)
    var defaultLossPercentage: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_sales_commission_percentage", precision = 5, scale = 2)
    var defaultSalesCommissionPercentage: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_card_fee_percentage", precision = 5, scale = 2)
    var defaultCardFeePercentage: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_tax_percentage", precision = 5, scale = 2)
    var defaultTaxPercentage: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_packaging_value", precision = 19, scale = 2)
    var defaultPackagingValue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_delivery_value", precision = 19, scale = 2)
    var defaultDeliveryValue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "default_average_items_per_order", precision = 10, scale = 2)
    var defaultAverageItemsPerOrder: BigDecimal = BigDecimal.ONE,

    @Column(name = "default_fixed_expense_percentage", precision = 5, scale = 2)
    var defaultFixedExpensePercentage: BigDecimal = BigDecimal("20"),

    @Column(name = "default_profit_margin_percentage", precision = 5, scale = 2)
    var defaultProfitMarginPercentage: BigDecimal = BigDecimal("25"),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "payment_method_config")
class PaymentMethodConfig(
    @Column(name = "payment_method", nullable = false, unique = true, length = 50)
    var paymentMethod: String,

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    var discountPercentage: BigDecimal? = null,

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    var feePercentage: BigDecimal? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "credit_card_installment_fee")
class CreditCardInstallmentFee(
    @Column(nullable = false)
    var installments: Int,

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    var feePercentage: BigDecimal? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

enum class ABCClass { A, B, C }
enum class XYZClass { X, Y, Z }

@Entity
@Table(name = "margin_strategies")
class MarginStrategy(
    @Column(name = "abc_faturamento", length = 1)
    @Enumerated(EnumType.STRING)
    var abcFaturamento: ABCClass? = null,

    @Column(name = "abc_margem", length = 1)
    @Enumerated(EnumType.STRING)
    var abcMargem: ABCClass? = null,

    @Column(name = "xyz_giro", length = 1)
    @Enumerated(EnumType.STRING)
    var xyzGiro: XYZClass? = null,

    @Column(name = "suggested_margin_percentage", precision = 5, scale = 2, nullable = false)
    var suggestedMarginPercentage: BigDecimal,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "sort_order")
    var sortOrder: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "pending_price_calculations")
class PendingPriceCalculation(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    var product: Product,

    @Column(name = "current_sale_price", precision = 19, scale = 2, nullable = false)
    var currentSalePrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "calculated_price", precision = 19, scale = 2, nullable = false)
    var calculatedPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_price", precision = 19, scale = 2)
    var finalPrice: BigDecimal? = null,

    @Column(name = "cmv", precision = 19, scale = 2, nullable = false)
    var cmv: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
