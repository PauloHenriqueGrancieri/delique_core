package com.delique.core.product.application.usecase

import com.delique.core.product.application.dto.ProductResponse
import com.delique.core.product.application.dto.toResponse
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.shared.domain.UseCase
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class UploadProductImageInput(
    val productId: Long,
    val imageData: ByteArray,
    val mediaType: String,
)

@Component
class UploadProductImageUseCase(
    private val productRepository: ProductRepository,
) : UseCase<UploadProductImageInput, ProductResponse> {

    @Transactional
    override fun execute(input: UploadProductImageInput): ProductResponse {
        val product = productRepository.findById(input.productId)
            ?: throw NotFoundException("Product not found: ${input.productId}")

        product.imageData      = input.imageData
        product.imageMediaType = input.mediaType
        product.imageUrl       = null

        return productRepository.save(product).toResponse()
    }
}
