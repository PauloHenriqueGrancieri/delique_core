package com.delique.core.product.domain.port

import com.delique.core.product.domain.model.ProductVariationOption

interface ProductVariationOptionRepository {
    fun findById(id: Long): ProductVariationOption?
    fun findByProductId(productId: Long): List<ProductVariationOption>
    fun findByProductIdAndId(productId: Long, id: Long): ProductVariationOption?
}
