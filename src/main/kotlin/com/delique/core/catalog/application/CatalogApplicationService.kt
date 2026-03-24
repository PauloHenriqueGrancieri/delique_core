package com.delique.core.catalog.application

import com.delique.core.catalog.application.dto.*
import com.delique.core.catalog.domain.model.Catalog
import com.delique.core.catalog.domain.model.CatalogPriceByPaymentMethod
import com.delique.core.catalog.domain.model.CatalogSettings
import com.delique.core.catalog.infrastructure.persistence.CatalogJpa
import com.delique.core.catalog.infrastructure.persistence.CatalogPriceByPaymentMethodJpa
import com.delique.core.catalog.infrastructure.persistence.CatalogSettingsJpa
import com.delique.core.inventory.application.ComboManagementService
import com.delique.core.inventory.application.StockManagementService
import com.delique.core.inventory.domain.port.ComboRepository
import com.delique.core.pricing.application.PaymentMethodApplicationService
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.port.ProductRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CatalogApplicationService(
    private val catalogJpa: CatalogJpa,
    private val catalogPriceByPaymentMethodJpa: CatalogPriceByPaymentMethodJpa,
    private val catalogSettingsJpa: CatalogSettingsJpa,
    private val productRepository: ProductRepository,
    private val paymentMethodApplicationService: PaymentMethodApplicationService,
    @Lazy private val stockManagementService: StockManagementService,
    @Lazy private val comboManagementService: ComboManagementService,
    private val comboRepository: ComboRepository,
) {
    private fun catalogDisplayIdComparator() = compareBy<Catalog>(
        { it.product.category.displayId ?: 9999 },
        { it.product.brand.displayId ?: 9999 },
        { it.product.displayId ?: 9999 },
    )

    private fun formattedDisplayId(p: Product): String? {
        val catId = p.category.displayId?.let { String.format("%02d", it) }
        val brandId = p.brand.displayId?.let { String.format("%02d", it) }
        val prodId = p.displayId?.let { String.format("%03d", it) }
        return if (catId != null && brandId != null && prodId != null) catId + brandId + prodId else null
    }

    private fun productImageUrl(productId: Long, p: Product): String? = when {
        p.imageData != null -> "/api/products/$productId/image"
        isStoredPath(p.imageUrl) -> "/api/products/$productId/image"
        else -> p.imageUrl
    }

    private fun isStoredPath(url: String?) =
        url != null && (url.startsWith("/uploads") || url.startsWith("uploads"))

    private fun variationImageUrl(productId: Long, optionId: Long, imageUrl: String?, imageData: ByteArray?) =
        when {
            imageData != null -> "/api/products/$productId/variation-options/$optionId/image"
            isStoredPath(imageUrl) -> "/api/products/$productId/variation-options/$optionId/image"
            else -> imageUrl
        }

    private fun stockQtyForProduct(productId: Long): Int {
        val combo = comboRepository.findByProductId(productId)
        return if (combo != null) {
            comboManagementService.getComboStock(combo.id)
        } else {
            try {
                stockManagementService.getCurrentStock(productId)
            } catch (_: Exception) {
                0
            }
        }
    }

    private fun comboItemsForProduct(productId: Long): List<ComboItemCatalogDto>? {
        val combo = comboRepository.findByProductIdWithItems(productId) ?: return null
        if (combo.items.isEmpty()) return null
        return combo.items.map { ComboItemCatalogDto(it.product.name, it.quantity) }
    }

    fun toCatalogItemDto(catalog: Catalog, stockQuantity: Int? = null): CatalogItemDto {
        val p = catalog.product
        val qty = stockQuantity ?: stockQtyForProduct(p.id)
        val catId = p.category.displayId?.let { String.format("%02d", it) }
        val brandId = p.brand.displayId?.let { String.format("%02d", it) }
        val prodId = p.displayId?.let { String.format("%03d", it) }
        val formatted = if (catId != null && brandId != null && prodId != null) catId + brandId + prodId else null
        return CatalogItemDto(
            id = catalog.id,
            productId = p.id,
            formattedDisplayId = formatted,
            productName = p.name,
            productDescription = p.description,
            costPrice = catalog.costPrice,
            salePrice = catalog.salePrice,
            discountPercentage = catalog.discountPercentage,
            finalPrice = catalog.finalPrice,
            imageUrl = productImageUrl(p.id, p),
            inCatalog = catalog.inCatalog,
            stockQuantity = qty,
            categoryName = p.category.name,
            categoryId = p.category.id,
            brandId = p.brand.id,
            brandName = p.brand.name,
            carouselOrder = catalog.carouselPosition,
            carouselDescription = catalog.carouselDescription,
            carouselShowPrice = catalog.carouselShowPrice ?: true,
            comboItems = comboItemsForProduct(p.id),
            variationType = p.variationType,
            variations = p.variationOptions.map { opt ->
                ProductVariationOptionCatalogDto(
                    id = opt.id,
                    name = opt.name,
                    imageUrl = variationImageUrl(p.id, opt.id, opt.imageUrl, opt.imageData),
                )
            }.takeIf { it.isNotEmpty() },
        )
    }

    fun getAllCatalogs(categoryId: Long?): List<CatalogItemDto> {
        val all = catalogJpa.findAllWithProductCategoryAndBrand().filter { it.inCatalog }
        val filtered = if (categoryId != null) all.filter { it.product.category.id == categoryId } else all
        return filtered.sortedWith(catalogDisplayIdComparator()).map { toCatalogItemDto(it) }
    }

    fun getCatalogByProductId(productId: Long): CatalogItemDto? {
        val product = productRepository.findById(productId) ?: return null
        val catalog = catalogJpa.findByProduct(product) ?: return null
        return toCatalogItemDto(catalog)
    }

    @Transactional
    fun createOrUpdateCatalog(dto: CatalogItemDto): CatalogItemDto {
        val product = productRepository.findById(dto.productId)
            ?: throw IllegalArgumentException("Product not found: ${dto.productId}")
        val finalPrice = if (dto.discountPercentage != null && dto.discountPercentage > BigDecimal.ZERO) {
            val discount = dto.discountPercentage.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
            dto.salePrice.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP)
        } else {
            dto.salePrice
        }
        val existing = catalogJpa.findByProduct(product)
        val catalog = existing ?: Catalog(
            product = product,
            costPrice = dto.costPrice,
            salePrice = dto.salePrice,
            discountPercentage = dto.discountPercentage,
            finalPrice = finalPrice,
            inCatalog = dto.inCatalog,
        )
        catalog.costPrice = dto.costPrice
        catalog.salePrice = dto.salePrice
        catalog.discountPercentage = dto.discountPercentage
        catalog.finalPrice = finalPrice
        catalog.inCatalog = dto.inCatalog
        val saved = catalogJpa.save(catalog)
        return toCatalogItemDto(saved)
    }

    @Transactional
    fun updateSalePrice(productId: Long, salePrice: BigDecimal): CatalogItemDto {
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")
        val catalog = catalogJpa.findByProduct(product)
            ?: throw IllegalArgumentException("Catalog entry not found for product id: $productId")
        catalog.salePrice = salePrice
        catalog.finalPrice = catalog.calculateFinalPrice()
        return toCatalogItemDto(catalogJpa.save(catalog))
    }

    @Transactional
    fun removeFromCatalog(productId: Long): CatalogItemDto {
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")
        val catalog = catalogJpa.findByProduct(product)
            ?: throw IllegalArgumentException("Catalog entry not found for product id: $productId")
        catalog.inCatalog = false
        return toCatalogItemDto(catalogJpa.save(catalog))
    }

    fun getProductsOutOfCatalog(): List<CatalogItemDto> =
        catalogJpa.findAllWithProductCategoryAndBrand()
            .filter { !it.inCatalog }
            .sortedWith(catalogDisplayIdComparator())
            .map { toCatalogItemDto(it, stockQtyForProduct(it.product.id)) }

    fun getProductsAvailableToAdd(): List<CatalogItemDto> {
        val catalogProductIds = catalogJpa.findAllWithProductCategoryAndBrand().map { it.product.id }.toSet()
        val outOfCatalog = getProductsOutOfCatalog()
        val productsWithoutEntry = productRepository.findAllOrdered()
            .filter { it.id !in catalogProductIds }
            .map { p ->
                CatalogItemDto(
                    id = null,
                    productId = p.id,
                    formattedDisplayId = formattedDisplayId(p),
                    productName = p.name,
                    productDescription = p.description,
                    costPrice = null,
                    salePrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    discountPercentage = null,
                    finalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    imageUrl = p.imageUrl,
                    inCatalog = false,
                    stockQuantity = try {
                        stockManagementService.getCurrentStock(p.id)
                    } catch (_: Exception) {
                        0
                    },
                    categoryName = p.category.name,
                    categoryId = p.category.id,
                    brandId = p.brand.id,
                    brandName = p.brand.name,
                    carouselOrder = null,
                    carouselDescription = null,
                    carouselShowPrice = true,
                    comboItems = null,
                    variationType = p.variationType,
                    variations = p.variationOptions.map { opt ->
                        ProductVariationOptionCatalogDto(opt.id, opt.name, opt.imageUrl)
                    }.takeIf { it.isNotEmpty() },
                )
            }
        val combined = outOfCatalog + productsWithoutEntry
        return combined.sortedWith(
            compareBy(
                { it.categoryName ?: "" },
                { it.brandName ?: "" },
                { it.formattedDisplayId ?: it.productName },
            ),
        )
    }

    @Transactional
    fun addToCatalog(productId: Long): CatalogItemDto {
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")
        val catalog = catalogJpa.findByProduct(product)
            ?: throw IllegalArgumentException("Catalog entry not found for product id: $productId")
        catalog.inCatalog = true
        return toCatalogItemDto(catalogJpa.save(catalog))
    }

    @Transactional
    fun setCarousel(slots: List<CarouselSlotDto>) {
        if (slots.size > 10) {
            throw IllegalArgumentException("Carousel can have at most 10 products")
        }
        val allInCatalog = catalogJpa.findAllWithProductCategoryAndBrand().filter { it.inCatalog }
        allInCatalog.forEach {
            it.carouselPosition = null
            it.carouselDescription = null
            it.carouselShowPrice = true
        }
        catalogJpa.saveAll(allInCatalog)
        slots.forEachIndexed { index, slot ->
            try {
                val product = productRepository.findById(slot.productId) ?: return@forEachIndexed
                val catalog = catalogJpa.findByProduct(product) ?: return@forEachIndexed
                if (!catalog.inCatalog) return@forEachIndexed
                catalog.carouselPosition = index + 1
                catalog.carouselDescription = slot.description?.takeIf { it.isNotBlank() }
                catalog.carouselShowPrice = slot.showPrice != false
                catalogJpa.save(catalog)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    private val paymentMethodDisplayNames = mapOf(
        "MONEY" to "Dinheiro",
        "PIX" to "PIX",
        "CREDIT_CARD" to "Cartão de Crédito",
        "DEBIT_CARD" to "Cartão de Débito",
    )

    fun getPaymentPrices(productId: Long): List<CatalogPriceByPaymentMethodDto> {
        val product = productRepository.findById(productId) ?: return emptyList()
        val catalog = catalogJpa.findByProduct(product) ?: return emptyList()
        val basePrice = catalog.salePrice
        val methods = paymentMethodApplicationService.getAllPaymentMethods()
        val stored = catalogPriceByPaymentMethodJpa.findByProductId(productId)
            .groupBy { "${it.paymentMethod}_${it.installments ?: 0}" }
        val result = mutableListOf<CatalogPriceByPaymentMethodDto>()
        for (method in methods) {
            val name = paymentMethodDisplayNames[method.code] ?: method.code
            if (method.code == "CREDIT_CARD") {
                for (fee in method.installmentFees) {
                    val key = "CREDIT_CARD_${fee.installments}"
                    val existing = stored[key]?.firstOrNull()
                    val finalPrice = existing?.finalPrice ?: run {
                        var p = basePrice
                        if (fee.feePercentage > BigDecimal.ZERO) {
                            p = p.multiply(BigDecimal.ONE.add(fee.feePercentage.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                        }
                        method.discountPercentage?.let {
                            if (it > BigDecimal.ZERO) {
                                p = p.multiply(BigDecimal.ONE.subtract(it.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                            }
                        }
                        p.setScale(2, RoundingMode.HALF_UP)
                    }
                    result.add(
                        CatalogPriceByPaymentMethodDto(
                            id = existing?.id,
                            productId = productId,
                            paymentMethod = method.code,
                            paymentMethodName = "${name} ${fee.installments}x",
                            installments = fee.installments,
                            basePrice = basePrice,
                            finalPrice = finalPrice,
                        ),
                    )
                }
            } else {
                val key = "${method.code}_0"
                val existing = stored[key]?.firstOrNull()
                val finalPrice = existing?.finalPrice ?: run {
                    var p = basePrice
                    method.feePercentage?.let {
                        if (it > BigDecimal.ZERO) {
                            p = p.multiply(BigDecimal.ONE.add(it.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                        }
                    }
                    method.discountPercentage?.let {
                        if (it > BigDecimal.ZERO) {
                            p = p.multiply(BigDecimal.ONE.subtract(it.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                        }
                    }
                    p.setScale(2, RoundingMode.HALF_UP)
                }
                result.add(
                    CatalogPriceByPaymentMethodDto(
                        id = existing?.id,
                        productId = productId,
                        paymentMethod = method.code,
                        paymentMethodName = name,
                        installments = null,
                        basePrice = basePrice,
                        finalPrice = finalPrice,
                    ),
                )
            }
        }
        return result
    }

    @Transactional
    fun savePaymentPrices(productId: Long, request: SaveCatalogPaymentPricesRequestDto) {
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")
        catalogPriceByPaymentMethodJpa.deleteByProduct_Id(productId)
        for (item in request.prices) {
            catalogPriceByPaymentMethodJpa.save(
                CatalogPriceByPaymentMethod(
                    product = product,
                    paymentMethod = item.paymentMethod,
                    installments = item.installments,
                    basePrice = request.basePrice,
                    finalPrice = item.finalPrice,
                ),
            )
        }
    }

    fun getSettings(): CatalogSettingsDto {
        val s = catalogSettingsJpa.findAll().firstOrNull() ?: return CatalogSettingsDto()
        return CatalogSettingsDto(
            whatsappNumber = s.whatsappNumber,
            instagramHandle = s.instagramHandle,
            address = s.address,
            logoUrl = s.logoUrl,
            catalogTitle = s.catalogTitle,
            primaryColor = s.primaryColor,
            aboutText = s.aboutText,
            showPrices = s.showPrices,
            showCart = s.showCart,
        )
    }

    @Transactional
    fun saveSettings(dto: CatalogSettingsDto): CatalogSettingsDto {
        val s = catalogSettingsJpa.findAll().firstOrNull() ?: CatalogSettings()
        s.whatsappNumber = dto.whatsappNumber?.takeIf { it.isNotBlank() }
        s.instagramHandle = dto.instagramHandle?.takeIf { it.isNotBlank() }
        s.address = dto.address?.takeIf { it.isNotBlank() }
        s.logoUrl = dto.logoUrl?.takeIf { it.isNotBlank() }
        s.catalogTitle = dto.catalogTitle?.takeIf { it.isNotBlank() }
        s.primaryColor = dto.primaryColor?.takeIf { it.isNotBlank() }
        s.aboutText = dto.aboutText?.takeIf { it.isNotBlank() }
        s.showPrices = dto.showPrices
        s.showCart = dto.showCart
        val saved = catalogSettingsJpa.save(s)
        return CatalogSettingsDto(
            whatsappNumber = saved.whatsappNumber,
            instagramHandle = saved.instagramHandle,
            address = saved.address,
            logoUrl = saved.logoUrl,
            catalogTitle = saved.catalogTitle,
            primaryColor = saved.primaryColor,
            aboutText = saved.aboutText,
            showPrices = saved.showPrices,
            showCart = saved.showCart,
        )
    }
}
