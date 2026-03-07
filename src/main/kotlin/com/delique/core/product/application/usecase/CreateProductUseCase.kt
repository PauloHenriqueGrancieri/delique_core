package com.delique.core.product.application.usecase

import com.delique.core.product.application.dto.ProductRequest
import com.delique.core.product.application.dto.ProductResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.port.BrandRepository
import com.delique.core.product.domain.port.CategoryRepository
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.shared.domain.UseCase
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateProductUseCase(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
) : UseCase<ProductRequest, ProductResponse> {

    @Transactional
    override fun execute(input: ProductRequest): ProductResponse {
        val category = categoryRepository.findById(input.categoryId)
            ?: throw NotFoundException("Category not found: ${input.categoryId}")
        val brand = brandRepository.findById(input.brandId)
            ?: throw NotFoundException("Brand not found: ${input.brandId}")

        val product = Product(
            name          = input.name,
            sku           = input.sku,
            description   = input.description,
            category      = category,
            brand         = brand,
            variationType = input.variationType,
            minimumStock  = input.minimumStock,
            displayId     = productRepository.nextDisplayId(),
        )
        return productRepository.save(product).toResponse()
    }
}
