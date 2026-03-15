package com.delique.core.inventory.infrastructure.integration

import com.delique.core.product.domain.model.Product
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "catalog")
class CatalogEntry(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    var product: Product,

    @Column(name = "cost_price", precision = 19, scale = 2)
    var costPrice: BigDecimal? = null,

    @Column(name = "sale_price", precision = 19, scale = 2, nullable = false)
    var salePrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    var discountPercentage: BigDecimal? = null,

    @Column(name = "final_price", precision = 19, scale = 2, nullable = false)
    var finalPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "in_catalog", nullable = false)
    var inCatalog: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}

@Entity
@Table(name = "price_calculation_config")
class PriceCalculationConfigRow(
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
    val id: Long = 0
}

@Entity
@Table(name = "pending_price_calculations")
class PendingPriceCalculationRow(
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
    val id: Long = 0
}
