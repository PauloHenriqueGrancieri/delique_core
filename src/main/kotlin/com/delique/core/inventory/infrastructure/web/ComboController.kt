package com.delique.core.inventory.infrastructure.web

import com.delique.core.inventory.application.ComboManagementService
import com.delique.core.inventory.application.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/combos")
class ComboController(
    private val comboManagementService: ComboManagementService,
) {
    @GetMapping
    fun getAllCombos(
        @RequestParam(required = false) activeOnly: Boolean?,
    ): ResponseEntity<List<ComboResponse>> =
        ResponseEntity.ok(comboManagementService.findAll(activeOnly))

    @GetMapping("/{id}")
    fun getComboById(@PathVariable id: Long): ResponseEntity<ComboResponse> =
        ResponseEntity.ok(comboManagementService.getById(id))

    @PostMapping("/preview-cost-and-prices")
    fun previewCostAndPrices(
        @RequestBody items: List<ComboPreviewItemRequest>,
    ): ResponseEntity<ComboPreviewCostAndPricesResponse> =
        ResponseEntity.ok(comboManagementService.previewCostAndPrices(items))

    @PostMapping
    fun createCombo(@RequestBody request: ComboCreateRequest): ResponseEntity<ComboResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(comboManagementService.create(request))

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createComboWithImage(
        @RequestPart("combo") request: ComboCreateRequest,
        @RequestPart(value = "image", required = false) image: MultipartFile?,
    ): ResponseEntity<ComboResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(comboManagementService.create(request, image))

    @PutMapping("/{id}")
    fun updateCombo(
        @PathVariable id: Long,
        @RequestBody request: ComboUpdateRequest,
    ): ResponseEntity<ComboResponse> =
        ResponseEntity.ok(comboManagementService.update(id, request))

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateComboWithImage(
        @PathVariable id: Long,
        @RequestPart("combo") request: ComboUpdateRequest,
        @RequestPart(value = "image", required = false) image: MultipartFile?,
    ): ResponseEntity<ComboResponse> =
        ResponseEntity.ok(comboManagementService.update(id, request, image))

    @DeleteMapping("/{id}")
    fun deleteCombo(@PathVariable id: Long): ResponseEntity<Void> {
        comboManagementService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/active")
    fun setComboActive(
        @PathVariable id: Long,
        @RequestParam active: Boolean,
    ): ResponseEntity<ComboResponse> =
        ResponseEntity.ok(comboManagementService.setActive(id, active))
}
