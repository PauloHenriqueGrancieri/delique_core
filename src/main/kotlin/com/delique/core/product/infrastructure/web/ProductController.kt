package com.delique.core.product.infrastructure.web

import com.delique.core.product.application.dto.ProductRequest
import com.delique.core.product.application.dto.ProductResponse
import com.delique.core.product.application.usecase.*
import com.delique.core.shared.infrastructure.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/products")
class ProductController(
    private val createProduct: CreateProductUseCase,
    private val updateProduct: UpdateProductUseCase,
    private val deleteProduct: DeleteProductUseCase,
    private val getProduct: GetProductUseCase,
    private val listProducts: ListProductsUseCase,
    private val uploadImage: UploadProductImageUseCase,
) {
    @GetMapping
    fun list(): List<ProductResponse> = listProducts.execute()

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<ProductResponse> = listProducts.search(q, PageRequest.of(page, size))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ProductResponse = getProduct.execute(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: ProductRequest): ProductResponse = createProduct.execute(request)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: ProductRequest): ProductResponse =
        updateProduct.execute(UpdateProductInput(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = deleteProduct.execute(id)

    @PostMapping("/{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(@PathVariable id: Long, @RequestParam file: MultipartFile): ProductResponse =
        uploadImage.execute(UploadProductImageInput(
            productId = id,
            imageData = file.bytes,
            mediaType = file.contentType ?: "application/octet-stream",
        ))

    @GetMapping("/{id}/image", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getImage(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val product = getProduct.execute(id)
        // Image retrieval handled via direct DB query in a real scenario
        return ResponseEntity.ok().build()
    }
}
