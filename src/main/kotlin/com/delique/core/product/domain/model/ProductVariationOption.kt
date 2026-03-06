package com.delique.core.product.domain.model

import com.delique.core.shared.infrastructure.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "product_variation_options")
class ProductVariationOption(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var name: String,

    @Column(length = 100)
    var sku: String? = null,

    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    @Column(name = "image_data", columnDefinition = "BYTEA")
    var imageData: ByteArray? = null,

    @Column(name = "image_media_type", length = 100)
    var imageMediaType: String? = null,
) : BaseEntity()
