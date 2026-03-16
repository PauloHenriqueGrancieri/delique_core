package com.delique.core.inventory.application.dto

import com.delique.core.inventory.domain.model.MovementType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class StockMovementResponse(
    val id: Long?,
    val productId: Long,
    val quantity: Int,
    val type: MovementType,
    val details: String? = null,
    val purchasePrice: BigDecimal? = null,
    val saleId: Long? = null,
    val editedAt: LocalDateTime? = null,
    val editReason: String? = null,
    val createdAt: LocalDateTime? = null,
    val expiresAt: LocalDate? = null,
    val variationOptionId: Long? = null,
    val variationOptionName: String? = null,
)

data class StockMovementRequest(
    val productId: Long,
    val quantity: Int,
    val details: String? = null,
    val purchasePrice: BigDecimal? = null,
    val expiresAt: LocalDate? = null,
    val variationOptionId: Long? = null,
)

data class StockAlertResponse(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val minimumStock: Int,
)

data class StockSummaryResponse(
    val productId: Long,
    val productName: String,
    val productDescription: String? = null,
    val formattedDisplayId: String? = null,
    val totalQuantity: Int,
    val totalCost: BigDecimal,
    val averageCost: BigDecimal,
    val batches: List<StockBatchResponse>,
    val units: List<StockUnitResponse>,
    val variationOptionId: Long? = null,
    val variationOptionName: String? = null,
)

data class StockBatchResponse(
    val movementId: Long,
    val quantity: Int,
    val purchasePrice: BigDecimal?,
    val createdAt: LocalDateTime,
    val units: List<StockUnitResponse>,
)

data class StockUnitResponse(
    val stockUnitId: String,
    val unitIndex: Int,
    val movementId: Long,
    val expiresAt: LocalDate? = null,
    val createdAt: LocalDateTime,
    val purchasePrice: BigDecimal? = null,
    val productCode: String = "",
    val productUnitNumber: Int = 0,
)

data class StockEntryResponse(
    val id: Long,
    val quantity: Int,
    val purchasePrice: BigDecimal?,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDate? = null,
    val unitIndex: Int? = null,
    val stockUnitId: String? = null,
)

data class ExpiringStockResponse(
    val productId: Long,
    val productName: String,
    val movementId: Long,
    val unitIndex: Int,
    val expiresAt: LocalDate,
    val daysRemaining: Int,
)

data class EditMovementRequest(
    val newQuantity: Int,
    val reason: String,
)

data class DeleteStockByIdsRequest(
    val ids: List<String>,
    val reason: String? = null,
)

data class UpdateUnitExpiryRequest(
    val expiresAt: String? = null,
)

data class UpdateMovementVariationRequest(
    val variationOptionId: Long? = null,
)
