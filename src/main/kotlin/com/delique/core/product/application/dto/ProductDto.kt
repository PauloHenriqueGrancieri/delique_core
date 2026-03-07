package com.delique.core.product.application.dto

import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption

data class ProductRequest(
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val categoryId: Long,
    val brandId: Long,
    val variationType: String? = null,
    val minimumStock: Int? = null,
)

data class VariationOptionRequest(
    val name: String,
    val sku: String? = null,
)

data class VariationOptionResponse(
    val id: Long,
    val name: String,
    val sku: String?,
    val imageUrl: String?,
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val sku: String?,
    val description: String?,
    val categoryId: Long,
    val categoryName: String,
    val brandId: Long,
    val brandName: String,
    val imageUrl: String?,
    val variationType: String?,
    val variationOptions: List<VariationOptionResponse>,
    val displayId: Int?,
    val minimumStock: Int?,
)

fun Product.toResponse() = ProductResponse(
    id               = id,
    name             = name,
    sku              = sku,
    description      = description,
    categoryId       = category.id,
    categoryName     = category.name,
    brandId          = brand.id,
    brandName        = brand.name,
    imageUrl         = imageUrl,
    variationType    = variationType,
    variationOptions = variationOptions.map { it.toResponse() },
    displayId        = displayId,
    minimumStock     = minimumStock,
)

fun ProductVariationOption.toResponse() = VariationOptionResponse(
    id       = id,
    name     = name,
    sku      = sku,
    imageUrl = imageUrl,
)
