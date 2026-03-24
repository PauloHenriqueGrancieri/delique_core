package com.delique.core.catalog.infrastructure.web

import com.delique.core.catalog.application.CatalogApplicationService
import com.delique.core.catalog.application.dto.*
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/catalog")
@CrossOrigin(origins = ["http://localhost:3000"])
class CatalogController(
    private val catalogApplicationService: CatalogApplicationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getAllCatalogs(@RequestParam(required = false) categoryId: Long?): ResponseEntity<List<CatalogItemDto>> =
        ResponseEntity.ok(catalogApplicationService.getAllCatalogs(categoryId))

    @GetMapping("/product/{productId}")
    fun getCatalogByProductId(@PathVariable productId: Long): ResponseEntity<CatalogItemDto> {
        val catalog = catalogApplicationService.getCatalogByProductId(productId)
        return if (catalog != null) ResponseEntity.ok(catalog) else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun createOrUpdateCatalog(@Valid @RequestBody dto: CatalogItemDto): ResponseEntity<CatalogItemDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(catalogApplicationService.createOrUpdateCatalog(dto))

    @PutMapping("/product/{productId}/sale-price")
    fun updateSalePrice(
        @PathVariable productId: Long,
        @RequestBody request: Map<String, BigDecimal>,
    ): ResponseEntity<CatalogItemDto> {
        val salePrice = request["salePrice"] ?: run {
            log.warn("Catalog sale price update: salePrice is required")
            throw IllegalArgumentException("salePrice is required")
        }
        return ResponseEntity.ok(catalogApplicationService.updateSalePrice(productId, salePrice))
    }

    @DeleteMapping("/product/{productId}")
    fun removeFromCatalog(@PathVariable productId: Long): ResponseEntity<CatalogItemDto> =
        ResponseEntity.ok(catalogApplicationService.removeFromCatalog(productId))

    @GetMapping("/out-of-catalog")
    fun getProductsOutOfCatalog(): ResponseEntity<List<CatalogItemDto>> =
        ResponseEntity.ok(catalogApplicationService.getProductsOutOfCatalog())

    @GetMapping("/available-to-add")
    fun getProductsAvailableToAdd(): ResponseEntity<List<CatalogItemDto>> =
        ResponseEntity.ok(catalogApplicationService.getProductsAvailableToAdd())

    @PostMapping("/product/{productId}/add")
    fun addToCatalog(@PathVariable productId: Long): ResponseEntity<CatalogItemDto> =
        ResponseEntity.ok(catalogApplicationService.addToCatalog(productId))

    @PutMapping("/carousel")
    fun setCarousel(@RequestBody request: CarouselRequestDto): ResponseEntity<Unit> {
        catalogApplicationService.setCarousel(request.slots)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/product/{productId}/payment-prices")
    fun getPaymentPrices(@PathVariable productId: Long): ResponseEntity<List<CatalogPriceByPaymentMethodDto>> =
        ResponseEntity.ok(catalogApplicationService.getPaymentPrices(productId))

    @PostMapping("/product/{productId}/payment-prices")
    fun savePaymentPrices(
        @PathVariable productId: Long,
        @Valid @RequestBody request: SaveCatalogPaymentPricesRequestDto,
    ): ResponseEntity<Unit> {
        catalogApplicationService.savePaymentPrices(productId, request)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/settings")
    fun getSettings(): ResponseEntity<CatalogSettingsDto> =
        ResponseEntity.ok(catalogApplicationService.getSettings())

    @PutMapping("/settings")
    fun saveSettings(@RequestBody dto: CatalogSettingsDto): ResponseEntity<CatalogSettingsDto> =
        ResponseEntity.ok(catalogApplicationService.saveSettings(dto))
}
