package com.delique.core.sales.application.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

data class SaleReturnItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val variationOption: String? = null,
)

data class SaleReturnRequest(
    val items: List<SaleReturnItemRequest>,
    val reason: String? = null,
)

data class SaleReturnItemDto(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val variationOption: String? = null,
)

data class SaleReturnDto(
    val id: Long,
    val orderId: Long,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val returnedAt: LocalDateTime,
    val reason: String? = null,
    val items: List<SaleReturnItemDto>,
    val totalRefunded: BigDecimal,
)
