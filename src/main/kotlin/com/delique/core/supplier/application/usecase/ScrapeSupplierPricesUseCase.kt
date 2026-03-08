package com.delique.core.supplier.application.usecase

import com.delique.core.supplier.domain.port.PriceScraperPort
import com.delique.core.supplier.domain.port.ProductSupplierRepository
import com.delique.core.supplier.domain.port.ScrapeRequest
import com.delique.core.supplier.domain.port.ScrapeResult
import com.delique.core.supplier.domain.port.SupplierRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ScrapeSupplierPricesUseCase(
    private val supplierRepository: SupplierRepository,
    private val productSupplierRepository: ProductSupplierRepository,
    private val priceScraper: PriceScraperPort,
) {
    @Transactional
    fun execute(supplierId: Long? = null): List<ScrapeResult> {
        val suppliers = if (supplierId != null)
            listOfNotNull(supplierRepository.findById(supplierId))
        else
            supplierRepository.findAllActive()

        val requests = suppliers
            .filter { !it.scraperExcluded }
            .flatMap { supplier ->
                productSupplierRepository.findBySupplierId(supplier.id)
                    .filter { it.url != null && !it.outOfStockAtSupplier }
                    .map { ps ->
                        ScrapeRequest(
                            url                = ps.url!!,
                            supplierId         = supplier.id,
                            productSupplierId  = ps.id,
                            successSelectors   = supplier.scraperSuccessSelectors,
                        )
                    }
            }

        val results = priceScraper.scrapeAll(requests)

        results.filter { it.success && it.scrapedPrice != null }.forEach { result ->
            val ps = productSupplierRepository.findById(result.productSupplierId) ?: return@forEach
            ps.price = result.scrapedPrice
            productSupplierRepository.save(ps)
        }

        return results
    }
}
