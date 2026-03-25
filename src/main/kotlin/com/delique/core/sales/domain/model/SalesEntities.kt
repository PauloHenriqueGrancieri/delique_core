package com.delique.core.sales.domain.model

import com.delique.core.marketing.domain.model.MarketingCampaign
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "clients")
class Client(
    @Column(nullable = false) var name: String,
    @Column(nullable = false) var phone: String,
    var age: Int? = null,
    var location: String? = null,
    var canal: String? = null,
    @Column(columnDefinition = "TEXT") var interests: String? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "orders")
class CustomerOrder(
    @Column(name = "payment_method", nullable = false) var paymentMethod: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") var client: Client? = null,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "order_discount_value", precision = 19, scale = 2) var orderDiscountValue: BigDecimal? = null,
    @Column(name = "fee_percentage", precision = 5, scale = 2) var feePercentage: BigDecimal? = null,
    @Column(name = "fee_value", precision = 19, scale = 2) var feeValue: BigDecimal? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "campaign_id") var campaign: MarketingCampaign? = null,
    @OneToMany(mappedBy = "customerOrder", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var sales: MutableList<SaleLine> = mutableListOf(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "sales")
class SaleLine(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) var product: Product,
    @Column(nullable = false) var quantity: Int,
    @Column(name = "unit_price", precision = 19, scale = 2, nullable = false) var unitPrice: BigDecimal,
    @Column(precision = 19, scale = 2) var discount: BigDecimal? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false)
    var customerOrder: CustomerOrder,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variation_option_id") var variationOption: ProductVariationOption? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "sale_returns")
class SaleReturn(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false)
    var customerOrder: CustomerOrder,
    @Column(name = "returned_at", nullable = false) var returnedAt: LocalDateTime = LocalDateTime.now(),
    @Column(columnDefinition = "TEXT") var reason: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    @OneToMany(mappedBy = "saleReturn", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<SaleReturnLine> = mutableListOf(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "sale_return_items")
class SaleReturnLine(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_return_id", nullable = false) var saleReturn: SaleReturn,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) var product: Product,
    @Column(nullable = false) var quantity: Int,
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2) var unitPrice: BigDecimal,
    @Column(name = "variation_option") var variationOption: String? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
