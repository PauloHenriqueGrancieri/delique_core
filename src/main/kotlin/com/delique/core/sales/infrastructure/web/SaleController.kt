package com.delique.core.sales.infrastructure.web

import com.delique.core.sales.application.SaleApplicationService
import com.delique.core.sales.application.dto.MultipleSalesDto
import com.delique.core.sales.application.dto.OrderDto
import com.delique.core.sales.application.dto.SaleLineDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sales")
@CrossOrigin(origins = ["http://localhost:3000"])
class SaleController(
    private val saleApplicationService: SaleApplicationService,
) {
    @GetMapping
    fun getAllOrders(): ResponseEntity<List<OrderDto>> =
        ResponseEntity.ok(saleApplicationService.getAllOrders())

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: Long): ResponseEntity<OrderDto> =
        ResponseEntity.ok(saleApplicationService.getOrderById(id))

    @PostMapping("/multiple")
    fun createMultipleSales(@Valid @RequestBody dto: MultipleSalesDto): ResponseEntity<OrderDto> {
        val items = dto.items.map { item ->
            SaleLineDto(
                productId = item.productId,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discount = item.discount,
                variationOptionId = item.variationOptionId,
            )
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            saleApplicationService.createOrderWithSales(
                items = items,
                paymentMethod = dto.paymentMethod,
                clientId = dto.clientId,
                orderDiscountValue = dto.orderDiscountValue,
                orderDiscountPercent = dto.orderDiscountPercent,
                installments = dto.installments,
                campaignId = dto.campaignId,
            ),
        )
    }

    @PutMapping("/{orderId}")
    fun updateOrder(
        @PathVariable orderId: Long,
        @Valid @RequestBody dto: MultipleSalesDto,
    ): ResponseEntity<OrderDto> {
        val items = dto.items.map { item ->
            SaleLineDto(
                productId = item.productId,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discount = item.discount,
                variationOptionId = item.variationOptionId,
            )
        }
        return ResponseEntity.ok(
            saleApplicationService.updateOrder(
                orderId = orderId,
                items = items,
                paymentMethod = dto.paymentMethod,
                clientId = dto.clientId,
                orderDiscountValue = dto.orderDiscountValue,
                orderDiscountPercent = dto.orderDiscountPercent,
                installments = dto.installments,
                createdAt = dto.createdAt,
                campaignId = dto.campaignId,
            ),
        )
    }
}
