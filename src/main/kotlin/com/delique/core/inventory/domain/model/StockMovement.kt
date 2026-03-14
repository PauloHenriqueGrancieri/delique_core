package com.delique.core.inventory.domain.model

import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class MovementType {
    ENTRY,
    SALE,
    DELETE,
    RETURN,
}

@Entity
@Table(name = "stock_movements")
class StockMovement(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: MovementType,

    @Column(columnDefinition = "TEXT")
    var details: String? = null,

    @Column(name = "purchase_price", precision = 19, scale = 2)
    var purchasePrice: BigDecimal? = null,

    @Column(name = "sale_id")
    var saleId: Long? = null,

    @Column(name = "edited_at")
    var editedAt: LocalDateTime? = null,

    @Column(name = "edit_reason", columnDefinition = "TEXT")
    var editReason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at")
    var expiresAt: LocalDate? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variation_option_id")
    var variationOption: ProductVariationOption? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
