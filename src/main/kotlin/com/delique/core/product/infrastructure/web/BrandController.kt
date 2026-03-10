package com.delique.core.product.infrastructure.web

import com.delique.core.product.application.dto.BrandRequest
import com.delique.core.product.application.dto.BrandResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.model.Brand
import com.delique.core.product.domain.port.BrandRepository
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/brands")
class BrandController(private val brandRepository: BrandRepository) {

    @GetMapping
    fun list(): List<BrandResponse> = brandRepository.findAllOrdered().map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: BrandRequest): BrandResponse {
        val brand = Brand(name = request.name, displayId = brandRepository.nextDisplayId())
        return brandRepository.save(brand).toResponse()
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: BrandRequest): BrandResponse {
        val brand = brandRepository.findById(id) ?: throw NotFoundException("Brand not found: $id")
        brand.name = request.name
        return brandRepository.save(brand).toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        if (brandRepository.findById(id) == null) throw NotFoundException("Brand not found: $id")
        brandRepository.delete(id)
    }
}
