package com.delique.core.supplier.infrastructure.web

import com.delique.core.supplier.application.usecase.ScrapeSupplierPricesUseCase
import com.delique.core.supplier.domain.port.ScrapeResult
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/price-scraper")
class PriceScraperController(
    private val scrapeUseCase: ScrapeSupplierPricesUseCase,
) {
    @PostMapping("/run")
    fun runAll(): List<ScrapeResult> = scrapeUseCase.execute()

    @PostMapping("/run/{supplierId}")
    fun runForSupplier(@PathVariable supplierId: Long): List<ScrapeResult> =
        scrapeUseCase.execute(supplierId)
}
