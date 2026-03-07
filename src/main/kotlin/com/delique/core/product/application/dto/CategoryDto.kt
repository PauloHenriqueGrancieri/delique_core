package com.delique.core.product.application.dto

import com.delique.core.product.domain.model.Category

data class CategoryRequest(val name: String, val hasValidity: Boolean = false)

data class CategoryResponse(val id: Long, val name: String, val hasValidity: Boolean, val displayId: Int?)

fun Category.toResponse() = CategoryResponse(
    id          = id,
    name        = name,
    hasValidity = hasValidity,
    displayId   = displayId,
)
