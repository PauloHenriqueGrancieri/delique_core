package com.delique.core.supplier.application.dto

import java.math.BigDecimal

data class PromotionResultDto(
    val productId: Long,
    val productName: String,
    val supplierId: Long,
    val supplierName: String,
    val currentPrice: BigDecimal,
    val lastPurchasePrice: BigDecimal,
    val discountPercent: BigDecimal,
    val url: String?,
)

data class PromotionScanStatusDto(
    val scanning: Boolean,
    val lastScanAt: String?,
    val promotionCount: Int,
)
