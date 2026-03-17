package com.delique.core.inventory.infrastructure.web

import com.delique.core.inventory.application.PurchaseOrderManagementService
import com.delique.core.inventory.application.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/purchase-orders")
class PurchaseOrderController(
    private val purchaseOrderManagementService: PurchaseOrderManagementService,
) {
    @GetMapping
    fun getAll(): ResponseEntity<List<PurchaseOrderResponse>> =
        ResponseEntity.ok(purchaseOrderManagementService.getAll())

    @GetMapping("/pending")
    fun getPending(): ResponseEntity<List<PurchaseOrderResponse>> =
        ResponseEntity.ok(purchaseOrderManagementService.getPending())

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(purchaseOrderManagementService.getById(id))

    @PostMapping
    fun create(@RequestBody dto: CreatePurchaseOrderRequest): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderManagementService.create(dto))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: UpdatePurchaseOrderRequest,
    ): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(purchaseOrderManagementService.update(id, dto))

    @PostMapping("/{id}/confirm")
    fun confirmDelivery(
        @PathVariable id: Long,
        @RequestBody dto: ConfirmDeliveryRequest,
    ): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(purchaseOrderManagementService.confirmDelivery(id, dto))

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(purchaseOrderManagementService.cancel(id))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        purchaseOrderManagementService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
