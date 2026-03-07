package com.delique.core.product.application.usecase

import com.delique.core.product.application.dto.ProductRequest
import com.delique.core.product.application.dto.ProductResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.port.BrandRepository
import com.delique.core.product.domain.port.CategoryRepository
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.shared.domain.UseCase
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class UpdateProductInput(val id: Long, val request: ProductRequest)

@Component
class UpdateProductUseCase(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
) : UseCase<UpdateProductInput, ProductResponse> {

    @Transactional
    override fun execute(input: UpdateProductInput): ProductResponse {
        val product = productRepository.findById(input.id)
            ?: throw NotFoundException("Product not found: ${input.id}")
        val category = categoryRepository.findById(input.request.categoryId)
            ?: throw NotFoundException("Category not found: ${input.request.categoryId}")
        val brand = brandRepository.findById(input.request.brandId)
            ?: throw NotFoundException("Brand not found: ${input.request.brandId}")

        product.name          = input.request.name
        product.sku           = input.request.sku
        product.description   = input.request.description
        product.category      = category
        product.brand         = brand
        product.variationType = input.request.variationType
        product.minimumStock  = input.request.minimumStock

        return productRepository.save(product).toResponse()
    }
}
