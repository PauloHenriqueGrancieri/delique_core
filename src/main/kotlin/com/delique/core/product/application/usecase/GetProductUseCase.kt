package com.delique.core.product.application.usecase

import com.delique.core.product.application.dto.ProductResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.shared.domain.UseCase
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductUseCase(
    private val productRepository: ProductRepository,
) : UseCase<Long, ProductResponse> {

    @Transactional(readOnly = true)
    override fun execute(input: Long): ProductResponse =
        (productRepository.findById(input) ?: throw NotFoundException("Product not found: $input")).toResponse()
}
