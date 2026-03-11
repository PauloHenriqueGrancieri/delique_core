package com.delique.core.supplier.application.dto

import com.delique.core.supplier.domain.model.Supplier
import java.math.BigDecimal

data class SupplierRequest(
    val name: String,
    val website: String? = null,
    val emails: List<String> = emptyList(),
    val phones: List<String> = emptyList(),
    val freight: BigDecimal = BigDecimal.ZERO,
    val minFreeFreight: BigDecimal = BigDecimal.ZERO,
    val minOrderValue: BigDecimal = BigDecimal.ZERO,
    val scraperExcluded: Boolean = false,
    val scraperSuccessSelectors: String? = null,
)

data class SupplierResponse(
    val id: Long,
    val name: String,
    val website: String?,
    val emails: List<String>,
    val phones: List<String>,
    val freight: BigDecimal,
    val minFreeFreight: BigDecimal,
    val minOrderValue: BigDecimal,
    val scraperExcluded: Boolean,
    val scraperSuccessSelectors: String?,
)

data class ProductSupplierRequest(
    val supplierId: Long,
    val url: String? = null,
    val price: BigDecimal? = null,
    val outOfStockAtSupplier: Boolean = false,
)

fun Supplier.toResponse() = SupplierResponse(
    id                     = id,
    name                   = name,
    website                = website,
    emails                 = emails.toList(),
    phones                 = phones.toList(),
    freight                = freight,
    minFreeFreight         = minFreeFreight,
    minOrderValue          = minOrderValue,
    scraperExcluded        = scraperExcluded,
    scraperSuccessSelectors = scraperSuccessSelectors,
)
