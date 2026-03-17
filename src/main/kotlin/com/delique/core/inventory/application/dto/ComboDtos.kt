package com.delique.core.inventory.application.dto

import java.math.BigDecimal

data class ComboItemRequest(
    val productId: Long,
    val quantity: Int,
)

data class ComboCreateRequest(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val items: List<ComboItemRequest>,
    val salePrice: BigDecimal,
    val discountPercentage: BigDecimal? = null,
    val maxAvailableQuantity: Int? = null,
    val active: Boolean = true,
)

data class ComboUpdateRequest(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val items: List<ComboItemRequest>,
    val salePrice: BigDecimal,
    val discountPercentage: BigDecimal? = null,
    val maxAvailableQuantity: Int? = null,
    val active: Boolean,
)

data class ComboItemResponse(
    val id: Long?,
    val productId: Long,
    val productName: String?,
    val quantity: Int,
)

data class ComboResponse(
    val id: Long,
    val productId: Long,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val salePrice: BigDecimal,
    val discountPercentage: BigDecimal? = null,
    val finalPrice: BigDecimal,
    val costPrice: BigDecimal? = null,
    val active: Boolean,
    val maxAvailableQuantity: Int? = null,
    val items: List<ComboItemResponse> = emptyList(),
    val stockQuantity: Int = 0,
    val formattedDisplayId: String? = null,
)

data class ComboPreviewItemRequest(
    val productId: Long,
    val quantity: Int,
)

data class ComboPreviewItemResponse(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val costPrice: BigDecimal?,
    val salePriceRef: BigDecimal?,
    val stockQuantity: Int = 0,
    val hasCost: Boolean,
    val hasSalePrice: Boolean,
    val valid: Boolean,
)

data class ComboPreviewCostAndPricesResponse(
    val items: List<ComboPreviewItemResponse>,
    val totalCost: BigDecimal,
    val totalSaleRef: BigDecimal,
    val suggestedPrice: BigDecimal?,
    val comboQuantityAvailable: Int,
    val canCreate: Boolean,
    val message: String? = null,
)
