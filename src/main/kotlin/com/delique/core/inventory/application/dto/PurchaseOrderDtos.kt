package com.delique.core.inventory.application.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class PurchaseOrderResponse(
    val id: Long,
    val totalFreight: BigDecimal,
    val status: String,
    val createdAt: LocalDateTime,
    val deliveredAt: LocalDateTime? = null,
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val items: List<PurchaseOrderItemResponse>,
)

data class PurchaseOrderItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productDescription: String? = null,
    val quantity: Int,
    val receivedQuantity: Int? = null,
    val unitCost: BigDecimal,
    val expiresAt: String? = null,
    val categoryHasValidity: Boolean = false,
    val variationOptionId: Long? = null,
    val variationOptionName: String? = null,
)

data class CreatePurchaseOrderRequest(
    val items: List<CreatePurchaseOrderItemRequest>,
    val totalFreight: BigDecimal,
    val supplierId: Long? = null,
)

data class CreatePurchaseOrderItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitCost: BigDecimal,
    val variationOptionId: Long? = null,
)

data class UpdatePurchaseOrderRequest(
    val items: List<CreatePurchaseOrderItemRequest>,
    val totalFreight: BigDecimal,
    val supplierId: Long? = null,
)

data class ReceivedItemBlockDto(
    val variationOptionId: Long? = null,
    val quantity: Int,
    val expiryDates: List<String> = emptyList(),
)

data class ConfirmDeliveryRequest(
    val deliveredAt: LocalDateTime,
    val itemExpiry: Map<String, List<String>> = emptyMap(),
    val receivedQuantities: Map<String, Int> = emptyMap(),
    val itemVariations: Map<String, Long> = emptyMap(),
    val receivedItemBlocks: Map<String, List<ReceivedItemBlockDto>> = emptyMap(),
)
