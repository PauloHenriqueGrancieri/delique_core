package com.delique.core.supplier.infrastructure.scraper

import com.delique.core.supplier.domain.port.ScrapeRequest
import com.delique.core.supplier.domain.port.ScrapeResult
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class JsoupScraperAdapter {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun scrape(request: ScrapeRequest): ScrapeResult {
        return try {
            val doc = Jsoup.connect(request.url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get()

            val selectors = request.successSelectors?.split(",")?.map { it.trim() }
                ?: listOf("[class*='price']", "[class*='preco']", "[itemprop='price']", ".product-price")

            val price = selectors.firstNotNullOfOrNull { selector ->
                runCatching {
                    doc.select(selector).first()?.text()
                        ?.replace(Regex("[^0-9,.]"), "")
                        ?.replace(",", ".")
                        ?.let { BigDecimal(it) }
                }.getOrNull()
            }

            ScrapeResult(request.productSupplierId, request.url, price, price != null)
        } catch (e: Exception) {
            log.warn("JSoup scrape failed for ${request.url}: ${e.message}")
            ScrapeResult(request.productSupplierId, request.url, null, false, e.message)
        }
    }
}
