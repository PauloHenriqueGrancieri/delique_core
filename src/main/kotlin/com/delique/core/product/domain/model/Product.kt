package com.delique.core.product.domain.model

import com.delique.core.shared.domain.AggregateRoot
import com.delique.core.shared.infrastructure.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "products")
class Product(
    @Column(nullable = false)
    var name: String,

    @Column(length = 100)
    var sku: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    var brand: Brand,

    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    @Column(name = "image_data", columnDefinition = "BYTEA")
    var imageData: ByteArray? = null,

    @Column(name = "image_media_type", length = 100)
    var imageMediaType: String? = null,

    @Column(name = "variation_type", length = 100)
    var variationType: String? = null,

    @Column(name = "display_id")
    var displayId: Int? = null,

    @Column(name = "minimum_stock")
    var minimumStock: Int? = null,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var variationOptions: MutableList<ProductVariationOption> = mutableListOf(),
) : BaseEntity()
