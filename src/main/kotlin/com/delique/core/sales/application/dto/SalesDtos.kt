package com.delique.core.sales.application.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ClientDto(
    val id: Long? = null,
    val name: String,
    val phone: String,
    val age: Int? = null,
    val location: String? = null,
    val canal: String? = null,
    val interests: String? = null,
)

data class SaleLineDto(
    val id: Long? = null,
    val productId: Long,
    val productName: String? = null,
    val productDescription: String? = null,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val discount: BigDecimal? = null,
    val orderId: Long? = null,
    val createdAt: LocalDateTime? = null,
    val variationOptionId: Long? = null,
    val variationOptionName: String? = null,
    val brandId: Long? = null,
    val brandName: String? = null,
)

data class OrderDto(
    val id: Long? = null,
    val paymentMethod: String,
    val clientId: Long? = null,
    val createdAt: LocalDateTime? = null,
    val orderDiscountValue: BigDecimal? = null,
    val feePercentage: BigDecimal? = null,
    val feeValue: BigDecimal? = null,
    val campaignId: Long? = null,
    val campaignName: String? = null,
    val items: List<SaleLineDto> = emptyList(),
)

data class SaleItemRequestDto(
    val productId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val discount: BigDecimal? = null,
    val variationOptionId: Long? = null,
)

data class MultipleSalesDto(
    val items: List<SaleItemRequestDto>,
    val paymentMethod: String,
    val clientId: Long? = null,
    val orderDiscountValue: BigDecimal? = null,
    val orderDiscountPercent: BigDecimal? = null,
    val installments: Int? = null,
    val createdAt: LocalDateTime? = null,
    val campaignId: Long? = null,
)
