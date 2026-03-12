package com.delique.core.supplier.infrastructure.scraper

import com.delique.core.supplier.domain.port.PriceScraperPort
import com.delique.core.supplier.domain.port.ScrapeRequest
import com.delique.core.supplier.domain.port.ScrapeResult
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PlaywrightScraperAdapter(
    private val browser: Browser?,
) : PriceScraperPort {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun scrape(request: ScrapeRequest): ScrapeResult {
        if (browser == null) {
            return ScrapeResult(request.productSupplierId, request.url, null, false, "Playwright not available")
        }

        var context: BrowserContext? = null
        return try {
            context = browser.newContext()
            val page = context.newPage()
            page.navigate(request.url)
            page.waitForLoadState()

            val price = extractPrice(page, request.successSelectors)
            ScrapeResult(request.productSupplierId, request.url, price, price != null)
        } catch (e: Exception) {
            log.warn("Scrape failed for ${request.url}: ${e.message}")
            ScrapeResult(request.productSupplierId, request.url, null, false, e.message)
        } finally {
            context?.close()
        }
    }

    override fun scrapeAll(requests: List<ScrapeRequest>): List<ScrapeResult> =
        requests.map { scrape(it) }

    private fun extractPrice(page: com.microsoft.playwright.Page, selectors: String?): BigDecimal? {
        val candidateSelectors = selectors?.split(",")?.map { it.trim() }
            ?: listOf(
                "[class*='price']", "[class*='preco']", "[itemprop='price']",
                ".product-price", ".sale-price", "span.price",
            )

        for (selector in candidateSelectors) {
            try {
                val element = page.querySelector(selector) ?: continue
                val text = element.textContent()?.replace(Regex("[^0-9,.]"), "")
                    ?.replace(",", ".") ?: continue
                return BigDecimal(text)
            } catch (_: Exception) {}
        }
        return null
    }
}
