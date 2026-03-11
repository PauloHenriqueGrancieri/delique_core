package com.delique.core.supplier.domain.port

data class ScrapeRequest(
    val url: String,
    val supplierId: Long,
    val productSupplierId: Long,
    val successSelectors: String? = null,
)

data class ScrapeResult(
    val productSupplierId: Long,
    val url: String,
    val scrapedPrice: java.math.BigDecimal?,
    val success: Boolean,
    val error: String? = null,
)

interface PriceScraperPort {
    fun scrape(request: ScrapeRequest): ScrapeResult
    fun scrapeAll(requests: List<ScrapeRequest>): List<ScrapeResult>
}
