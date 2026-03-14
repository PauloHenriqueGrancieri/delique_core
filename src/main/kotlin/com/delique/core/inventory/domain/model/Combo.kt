package com.delique.core.inventory.domain.model

import com.delique.core.product.domain.model.Product
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "combos")
class Combo(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    var product: Product,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    @Column(name = "image_data", columnDefinition = "BYTEA")
    var imageData: ByteArray? = null,

    @Column(name = "image_media_type", length = 100)
    var imageMediaType: String? = null,

    @Column(name = "sale_price", precision = 19, scale = 2, nullable = false)
    var salePrice: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "max_available_quantity")
    var maxAvailableQuantity: Int? = null,

    @OneToMany(mappedBy = "combo", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<ComboItem> = mutableListOf(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}

@Entity
@Table(
    name = "combo_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["combo_id", "product_id"])],
)
class ComboItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    var combo: Combo,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var quantity: Int = 1,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}
