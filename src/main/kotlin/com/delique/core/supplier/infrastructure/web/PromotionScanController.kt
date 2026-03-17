package com.delique.core.supplier.infrastructure.web

import com.delique.core.supplier.application.PromotionScanApplicationService
import com.delique.core.supplier.application.dto.PromotionResultDto
import com.delique.core.supplier.application.dto.PromotionScanStatusDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/promotion-scan")
@CrossOrigin(origins = ["http://localhost:3000"])
class PromotionScanController(
    private val promotionScanApplicationService: PromotionScanApplicationService,
) {
    @PostMapping("/run")
    fun runScan(): ResponseEntity<Void> {
        promotionScanApplicationService.triggerScan()
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/status")
    fun getStatus(@RequestParam(defaultValue = "0") minDiscount: BigDecimal): ResponseEntity<PromotionScanStatusDto> =
        ResponseEntity.ok(promotionScanApplicationService.getStatus(minDiscount))

    @GetMapping("/results")
    fun getResults(@RequestParam(defaultValue = "0") minDiscount: BigDecimal): ResponseEntity<List<PromotionResultDto>> =
        ResponseEntity.ok(promotionScanApplicationService.getResults(minDiscount))
}
