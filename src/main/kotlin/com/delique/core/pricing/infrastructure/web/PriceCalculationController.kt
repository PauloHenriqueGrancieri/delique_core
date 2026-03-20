package com.delique.core.pricing.infrastructure.web

import com.delique.core.financial.application.FixedExpenseCalculationService
import com.delique.core.pricing.application.PriceCalculationApplicationService
import com.delique.core.pricing.application.dto.PriceCalculationRequest
import com.delique.core.pricing.application.dto.PriceCalculationResponse
import com.delique.core.pricing.domain.model.PriceCalculationConfig
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/price-calculation")
@CrossOrigin(origins = ["http://localhost:3000"])
class PriceCalculationController(
    private val priceCalculationApplicationService: PriceCalculationApplicationService,
    private val fixedExpenseCalculationService: FixedExpenseCalculationService,
) {
    @GetMapping("/defaults")
    fun getDefaultConfig(): ResponseEntity<PriceCalculationConfig> =
        ResponseEntity.ok(priceCalculationApplicationService.getDefaultConfig())

    @GetMapping("/fixed-expense-pct")
    fun getFixedExpensePct(@RequestParam(defaultValue = "3") months: Int): ResponseEntity<Map<String, java.math.BigDecimal>> {
        val safeMonths = months.coerceIn(1, 24)
        val pct = fixedExpenseCalculationService.calculateFixedExpensePercentageOverRevenue(safeMonths)
        return ResponseEntity.ok(
            mapOf(
                "percentage" to pct,
                "months" to java.math.BigDecimal(safeMonths),
            ),
        )
    }

    @GetMapping("/purchase-cost/{productId}")
    fun getPurchaseCostFromStock(@PathVariable productId: Long): ResponseEntity<Map<String, java.math.BigDecimal>> {
        val cost = priceCalculationApplicationService.getPurchaseCostFromStock(productId)
        return ResponseEntity.ok(mapOf("cmv" to cost))
    }

    @GetMapping("/average-items-per-order")
    fun getAverageItemsPerOrder(): ResponseEntity<Map<String, java.math.BigDecimal>> {
        val average = priceCalculationApplicationService.calculateAverageItemsPerOrder()
        return ResponseEntity.ok(mapOf("averageItemsPerOrder" to average))
    }

    @PutMapping("/config")
    fun updateDefaultConfig(@RequestBody config: PriceCalculationConfig): ResponseEntity<PriceCalculationConfig> =
        ResponseEntity.ok(priceCalculationApplicationService.updateDefaultConfig(config))

    @PostMapping("/calculate")
    fun calculatePrice(@RequestBody request: PriceCalculationRequest): ResponseEntity<PriceCalculationResponse> =
        ResponseEntity.ok(priceCalculationApplicationService.calculatePrice(request))

    @PostMapping("/save-to-catalog")
    fun saveToCatalog(@RequestBody response: PriceCalculationResponse) =
        ResponseEntity.ok(priceCalculationApplicationService.saveToCatalog(response))

    @GetMapping("/analytics/{productId}")
    fun getProductAnalytics(@PathVariable productId: Long) =
        priceCalculationApplicationService.getProductAnalytics(productId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @GetMapping("/pending")
    fun getPendingPriceCalculations() =
        ResponseEntity.ok(priceCalculationApplicationService.getAllPendingPriceCalculations())

    @PostMapping("/pending/{id}/approve")
    fun approvePendingPriceCalculation(
        @PathVariable id: Long,
        @RequestBody request: Map<String, java.math.BigDecimal?>,
    ) = ResponseEntity.ok(
        priceCalculationApplicationService.approvePendingPriceCalculation(id, request["finalPrice"]),
    )

    @DeleteMapping("/pending/{id}")
    fun rejectPendingPriceCalculation(@PathVariable id: Long): ResponseEntity<Void> {
        priceCalculationApplicationService.rejectPendingPriceCalculation(id)
        return ResponseEntity.noContent().build()
    }
}
