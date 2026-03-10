package com.delique.core.product.infrastructure.web

import com.delique.core.product.application.dto.CategoryRequest
import com.delique.core.product.application.dto.CategoryResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.model.Category
import com.delique.core.product.domain.port.CategoryRepository
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categories")
class CategoryController(private val categoryRepository: CategoryRepository) {

    @GetMapping
    fun list(): List<CategoryResponse> = categoryRepository.findAll().map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CategoryRequest): CategoryResponse {
        val category = Category(name = request.name, hasValidity = request.hasValidity)
        return categoryRepository.save(category).toResponse()
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: CategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id) ?: throw NotFoundException("Category not found: $id")
        category.name = request.name
        category.hasValidity = request.hasValidity
        return categoryRepository.save(category).toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        if (categoryRepository.findById(id) == null) throw NotFoundException("Category not found: $id")
        categoryRepository.delete(id)
    }
}
