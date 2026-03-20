package com.delique.core.pricing.infrastructure.web

import com.delique.core.pricing.application.PaymentMethodApplicationService
import com.delique.core.pricing.application.dto.PaymentMethodConfigBulkUpdateDto
import com.delique.core.pricing.application.dto.PaymentMethodDto
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payment-methods")
@CrossOrigin(origins = ["http://localhost:3000"])
class PaymentMethodController(
    private val paymentMethodApplicationService: PaymentMethodApplicationService,
) {
    @GetMapping
    fun getAllPaymentMethods(): ResponseEntity<List<PaymentMethodDto>> =
        ResponseEntity.ok(paymentMethodApplicationService.getAllPaymentMethods())

    @PutMapping("/config")
    fun updateConfig(@Valid @RequestBody body: PaymentMethodConfigBulkUpdateDto): ResponseEntity<Unit> {
        paymentMethodApplicationService.updateConfigBulk(body.configs)
        return ResponseEntity.ok().build()
    }
}
