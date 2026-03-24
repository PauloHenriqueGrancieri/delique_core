package com.delique.core.catalog.application.dto

import java.math.BigDecimal

data class CarouselSlotDto(
    val productId: Long,
    val description: String? = null,
    val showPrice: Boolean? = true,
)

data class CarouselRequestDto(
    val slots: List<CarouselSlotDto>,
)

/** Resposta mínima usada por price-calculation ao aprovar/salvar no catálogo */
data class CatalogDto(
    val id: Long? = null,
    val productId: Long,
    val productName: String? = null,
    val costPrice: BigDecimal? = null,
    val salePrice: BigDecimal = BigDecimal.ZERO,
    val discountPercentage: BigDecimal? = null,
    val finalPrice: BigDecimal = BigDecimal.ZERO,
    val inCatalog: Boolean = true,
)

data class ProductVariationOptionCatalogDto(
    val id: Long,
    val name: String,
    val imageUrl: String?,
)

data class ComboItemCatalogDto(
    val productName: String,
    val quantity: Int,
)

data class CatalogItemDto(
    val id: Long? = null,
    val productId: Long,
    val formattedDisplayId: String? = null,
    val productName: String? = null,
    val productDescription: String? = null,
    val costPrice: BigDecimal? = null,
    val salePrice: BigDecimal,
    val discountPercentage: BigDecimal? = null,
    val finalPrice: BigDecimal,
    val imageUrl: String? = null,
    val inCatalog: Boolean = true,
    val stockQuantity: Int = 0,
    val categoryName: String? = null,
    val categoryId: Long? = null,
    val brandId: Long? = null,
    val brandName: String? = null,
    val carouselOrder: Int? = null,
    val carouselDescription: String? = null,
    val carouselShowPrice: Boolean? = true,
    val comboItems: List<ComboItemCatalogDto>? = null,
    val variationType: String? = null,
    val variations: List<ProductVariationOptionCatalogDto>? = null,
)

data class CatalogSettingsDto(
    val whatsappNumber: String? = null,
    val instagramHandle: String? = null,
    val address: String? = null,
    val logoUrl: String? = null,
    val catalogTitle: String? = null,
    val primaryColor: String? = null,
    val aboutText: String? = null,
    val showPrices: Boolean = true,
    val showCart: Boolean = true,
)

data class CatalogPriceByPaymentMethodDto(
    val id: Long? = null,
    val productId: Long,
    val paymentMethod: String,
    val paymentMethodName: String,
    val installments: Int? = null,
    val basePrice: BigDecimal,
    val finalPrice: BigDecimal,
)

data class SaveCatalogPaymentPriceItemDto(
    val paymentMethod: String,
    val installments: Int? = null,
    val finalPrice: BigDecimal,
)

data class SaveCatalogPaymentPricesRequestDto(
    val basePrice: BigDecimal,
    val prices: List<SaveCatalogPaymentPriceItemDto>,
)
