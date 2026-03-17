package com.delique.core.inventory.infrastructure.web

import com.delique.core.inventory.application.StockManagementService
import com.delique.core.inventory.application.StockReconciliationResult
import com.delique.core.inventory.application.StockReconciliationService
import com.delique.core.inventory.application.dto.*
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/stock")
class StockController(
    private val stockManagementService: StockManagementService,
    private val stockReconciliationService: StockReconciliationService,
) {
    @GetMapping
    fun getAllMovements(): ResponseEntity<List<StockMovementResponse>> =
        ResponseEntity.ok(stockManagementService.getAllMovements())

    @GetMapping("/history")
    fun getStockHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<StockMovementResponse>> =
        ResponseEntity.ok(stockManagementService.getStockHistory(page, size))

    @GetMapping("/product/{productId}")
    fun getMovementsByProduct(@PathVariable productId: Long): ResponseEntity<List<StockMovementResponse>> =
        ResponseEntity.ok(stockManagementService.getMovementsByProduct(productId))

    @GetMapping("/product/{productId}/current")
    fun getCurrentStock(@PathVariable productId: Long): ResponseEntity<Map<String, Int>> =
        ResponseEntity.ok(mapOf("currentStock" to stockManagementService.getCurrentStock(productId)))

    @GetMapping("/product/{productId}/summary")
    fun getStockSummary(@PathVariable productId: Long): ResponseEntity<StockSummaryResponse> =
        ResponseEntity.ok(stockManagementService.getStockSummary(productId))

    @GetMapping("/grouped")
    fun getGroupedStock(): ResponseEntity<List<StockSummaryResponse>> =
        ResponseEntity.ok(stockManagementService.getGroupedStock())

    @PostMapping("/entry")
    fun addStockEntry(@RequestBody dto: StockMovementRequest): ResponseEntity<StockMovementResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(stockManagementService.addStockEntry(dto))

    @PostMapping("/delete")
    fun deleteStock(@RequestBody dto: StockMovementRequest): ResponseEntity<StockMovementResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(stockManagementService.deleteStock(dto))

    @PostMapping("/delete-by-ids")
    fun deleteStockByIds(@RequestBody req: DeleteStockByIdsRequest): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(stockManagementService.deleteStockByIds(req.ids, req.reason))

    @PutMapping("/edit/{movementId}")
    fun editMovement(
        @PathVariable movementId: Long,
        @RequestBody request: EditMovementRequest,
    ): ResponseEntity<StockMovementResponse> =
        ResponseEntity.ok(stockManagementService.editMovement(movementId, request.newQuantity, request.reason))

    @PatchMapping("/movement/{movementId}/variation")
    fun updateMovementVariation(
        @PathVariable movementId: Long,
        @RequestBody request: UpdateMovementVariationRequest,
    ): ResponseEntity<StockMovementResponse> =
        ResponseEntity.ok(stockManagementService.updateMovementVariation(movementId, request.variationOptionId))

    @PatchMapping("/unit/{stockUnitId}/variation")
    fun updateUnitVariation(
        @PathVariable stockUnitId: String,
        @RequestBody request: UpdateMovementVariationRequest,
    ): ResponseEntity<StockMovementResponse> =
        ResponseEntity.ok(stockManagementService.updateUnitVariation(stockUnitId, request.variationOptionId))

    @PatchMapping("/entry/{movementId}/unit/{unitIndex}/expiry")
    fun updateUnitExpiry(
        @PathVariable movementId: Long,
        @PathVariable unitIndex: Int,
        @RequestBody request: UpdateUnitExpiryRequest,
    ): ResponseEntity<StockEntryResponse> {
        val expiresAt = parseExpiresAt(request.expiresAt)
        return ResponseEntity.ok(stockManagementService.updateUnitExpiry(movementId, unitIndex, expiresAt))
    }

    @PatchMapping("/unit/{stockUnitId}/expiry")
    fun updateUnitExpiryByStockUnitId(
        @PathVariable stockUnitId: String,
        @RequestBody request: UpdateUnitExpiryRequest,
    ): ResponseEntity<StockEntryResponse> {
        val expiresAt = parseExpiresAt(request.expiresAt)
        return ResponseEntity.ok(stockManagementService.updateUnitExpiryByStockUnitId(stockUnitId, expiresAt))
    }

    private fun parseExpiresAt(value: String?): LocalDate? {
        val s = value?.trim() ?: return null
        if (s.isEmpty()) return null
        return when {
            s.length == 7 && s[4] == '-' -> YearMonth.parse(s).atDay(1)
            else -> try {
                LocalDate.parse(s)
            } catch (_: Exception) {
                null
            }
        }
    }

    @GetMapping("/alerts")
    fun getStockAlerts(): ResponseEntity<List<StockAlertResponse>> =
        ResponseEntity.ok(stockManagementService.getProductsBelowMinimumStock())

    @GetMapping("/expiring")
    fun getExpiringStock(
        @RequestParam(defaultValue = "3") monthsThreshold: Int,
    ): ResponseEntity<List<ExpiringStockResponse>> =
        ResponseEntity.ok(stockManagementService.getExpiringStock(monthsThreshold))

    @PostMapping("/reconcile")
    fun reconcile(): ResponseEntity<StockReconciliationResult> =
        ResponseEntity.ok(stockReconciliationService.reconcile())
}
