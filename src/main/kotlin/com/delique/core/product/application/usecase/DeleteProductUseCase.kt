package com.delique.core.product.application.usecase

import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.shared.domain.NoOutputUseCase
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductUseCase(
    private val productRepository: ProductRepository,
) : NoOutputUseCase<Long> {

    @Transactional
    override fun execute(input: Long) {
        if (!productRepository.existsById(input)) throw NotFoundException("Product not found: $input")
        productRepository.delete(input)
    }
}
