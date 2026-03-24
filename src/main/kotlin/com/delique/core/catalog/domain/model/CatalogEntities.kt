package com.delique.core.catalog.domain.model

import com.delique.core.product.domain.model.Product
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "catalog")
class Catalog(
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false, unique = true)
    var product: Product,
    @Column(name = "cost_price", precision = 19, scale = 2) var costPrice: BigDecimal? = null,
    @Column(name = "sale_price", precision = 19, scale = 2, nullable = false) var salePrice: BigDecimal,
    @Column(name = "discount_percentage", precision = 5, scale = 2) var discountPercentage: BigDecimal? = null,
    @Column(name = "final_price", precision = 19, scale = 2, nullable = false) var finalPrice: BigDecimal,
    @Column(name = "in_catalog", nullable = false) var inCatalog: Boolean = true,
    @Column(name = "carousel_position") var carouselPosition: Int? = null,
    @Column(name = "carousel_description", columnDefinition = "TEXT") var carouselDescription: String? = null,
    @Column(name = "carousel_show_price") var carouselShowPrice: Boolean? = true,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    fun calculateFinalPrice(): BigDecimal {
        val discount = discountPercentage?.divide(BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
            ?: BigDecimal.ZERO
        return salePrice.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, java.math.RoundingMode.HALF_UP)
    }
}

@Entity
@Table(name = "catalog_settings")
class CatalogSettings(
    @Column(name = "whatsapp_number", length = 30) var whatsappNumber: String? = null,
    @Column(name = "instagram_handle", length = 100) var instagramHandle: String? = null,
    @Column(columnDefinition = "TEXT") var address: String? = null,
    @Column(name = "logo_url", columnDefinition = "TEXT") var logoUrl: String? = null,
    @Column(name = "catalog_title", length = 200) var catalogTitle: String? = null,
    @Column(name = "primary_color", length = 20) var primaryColor: String? = null,
    @Column(name = "about_text", columnDefinition = "TEXT") var aboutText: String? = null,
    @Column(name = "show_prices", nullable = false) var showPrices: Boolean = true,
    @Column(name = "show_cart", nullable = false) var showCart: Boolean = true,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(
    name = "catalog_price_by_payment_method",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "payment_method", "installments"])],
)
class CatalogPriceByPaymentMethod(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false)
    var product: Product,
    @Column(name = "payment_method", nullable = false, length = 50) var paymentMethod: String,
    var installments: Int? = null,
    @Column(name = "base_price", precision = 19, scale = 2, nullable = false) var basePrice: BigDecimal,
    @Column(name = "final_price", precision = 19, scale = 2, nullable = false) var finalPrice: BigDecimal,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
