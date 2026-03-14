package com.delique.core.sales.infrastructure.web

import com.delique.core.sales.application.SaleReturnApplicationService
import com.delique.core.sales.application.dto.SaleReturnDto
import com.delique.core.sales.application.dto.SaleReturnRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class SaleReturnController(
    private val saleReturnApplicationService: SaleReturnApplicationService,
) {
    @PostMapping("/orders/{orderId}/returns")
    fun processReturn(
        @PathVariable orderId: Long,
        @RequestBody request: SaleReturnRequest,
    ): ResponseEntity<SaleReturnDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(saleReturnApplicationService.processReturn(orderId, request))

    @GetMapping("/orders/{orderId}/returns")
    fun getReturnsByOrder(@PathVariable orderId: Long): ResponseEntity<List<SaleReturnDto>> =
        ResponseEntity.ok(saleReturnApplicationService.getReturnsByOrder(orderId))

    @GetMapping("/returns")
    fun getAllReturns(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?,
    ): ResponseEntity<List<SaleReturnDto>> =
        ResponseEntity.ok(saleReturnApplicationService.getAllReturns(startDate, endDate))
}
