package com.delique.core.product.application.usecase

import com.delique.core.product.application.dto.ProductResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.shared.domain.NoInputUseCase
import com.delique.core.shared.infrastructure.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ListProductsUseCase(
    private val productRepository: ProductRepository,
) {
    @Transactional(readOnly = true)
    fun execute(): List<ProductResponse> =
        productRepository.findAllOrdered().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun search(query: String, pageable: Pageable): PageResponse<ProductResponse> =
        PageResponse.of(productRepository.search(query, pageable).map { it.toResponse() })
}
