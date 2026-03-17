package com.delique.core.supplier.application

import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.supplier.application.dto.PromotionResultDto
import com.delique.core.supplier.application.dto.PromotionScanStatusDto
import com.delique.core.supplier.application.usecase.ScrapeSupplierPricesUseCase
import com.delique.core.supplier.domain.port.ProductSupplierRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class PromotionScanApplicationService(
    private val scrapeSupplierPricesUseCase: ScrapeSupplierPricesUseCase,
    private val productSupplierRepository: ProductSupplierRepository,
    private val stockMovementRepository: StockMovementRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var scanning = false

    @Volatile
    private var lastScanAt: LocalDateTime? = null

    @Volatile
    private var cachedResults: List<PromotionResultDto> = emptyList()

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        Thread { runScan() }.also { it.isDaemon = true }.start()
    }

    fun triggerScan() {
        if (scanning) return
        Thread { runScan() }.also { it.isDaemon = true }.start()
    }

    private fun runScan() {
        if (scanning) return
        scanning = true
        log.info("Promotion scan started")
        try {
            scrapeSupplierPricesUseCase.execute()

            val scrapeable = productSupplierRepository.findAllScrapeable()
            if (scrapeable.isEmpty()) {
                cachedResults = emptyList()
                lastScanAt = LocalDateTime.now()
                return
            }

            val lastPurchasePriceMap = stockMovementRepository
                .findLastPurchasePricePerProduct()
                .associate { it.product.id to it.purchasePrice!! }

            cachedResults = productSupplierRepository.findAllScrapeable()
                .mapNotNull { ps ->
                    val currentPrice = ps.price ?: return@mapNotNull null
                    val lastPP = lastPurchasePriceMap[ps.product.id] ?: return@mapNotNull null
                    if (currentPrice < lastPP) {
                        val discount = (lastPP - currentPrice)
                            .divide(lastPP, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal("100"))
                            .setScale(1, RoundingMode.HALF_UP)
                        PromotionResultDto(
                            productId = ps.product.id,
                            productName = ps.product.name,
                            supplierId = ps.supplier.id,
                            supplierName = ps.supplier.name,
                            currentPrice = currentPrice,
                            lastPurchasePrice = lastPP,
                            discountPercent = discount,
                            url = ps.url,
                        )
                    } else {
                        null
                    }
                }

            lastScanAt = LocalDateTime.now()
            log.info("Promotion scan completed: ${cachedResults.size} promotion(s) found")
        } catch (e: Exception) {
            log.error("Promotion scan failed", e)
        } finally {
            scanning = false
        }
    }

    fun getStatus(minDiscount: BigDecimal = BigDecimal.ZERO): PromotionScanStatusDto =
        PromotionScanStatusDto(
            scanning = scanning,
            lastScanAt = lastScanAt?.toString(),
            promotionCount = filtered(minDiscount).size,
        )

    fun getResults(minDiscount: BigDecimal = BigDecimal.ZERO): List<PromotionResultDto> =
        filtered(minDiscount)

    private fun filtered(minDiscount: BigDecimal) =
        if (minDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            cachedResults
        } else {
            cachedResults.filter { it.discountPercent >= minDiscount }
        }
}
