package com.delique.core.product.application.dto

import com.delique.core.product.domain.model.Brand

data class BrandRequest(val name: String)

data class BrandResponse(val id: Long, val name: String, val displayId: Int?)

fun Brand.toResponse() = BrandResponse(id = id, name = name, displayId = displayId)
