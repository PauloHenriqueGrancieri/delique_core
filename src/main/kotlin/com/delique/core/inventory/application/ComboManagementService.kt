package com.delique.core.inventory.application

import com.delique.core.inventory.application.dto.*
import com.delique.core.inventory.domain.model.Combo
import com.delique.core.inventory.domain.model.ComboItem
import com.delique.core.inventory.domain.port.ComboRepository
import com.delique.core.inventory.infrastructure.integration.CatalogEntry
import com.delique.core.inventory.infrastructure.integration.JpaCatalogEntryJpa
import com.delique.core.inventory.infrastructure.integration.CatalogPricingSupport
import com.delique.core.product.domain.model.Brand
import com.delique.core.product.domain.model.Category
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.port.BrandRepository
import com.delique.core.product.domain.port.CategoryRepository
import com.delique.core.product.domain.port.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ComboManagementService(
    private val comboRepository: ComboRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val catalogJpa: JpaCatalogEntryJpa,
    private val stockManagementService: StockManagementService,
    private val catalogPricingSupport: CatalogPricingSupport,
) {
    private fun getComboCategory(): Category =
        categoryRepository.findByName("Combos")
            ?: categoryRepository.save(
                Category(name = "Combos", hasValidity = false, displayId = categoryRepository.nextDisplayId()),
            )

    private fun getComboBrand(): Brand =
        brandRepository.findByName("Combos")
            ?: brandRepository.save(
                Brand(name = "Combos", displayId = brandRepository.nextDisplayId()),
            )

    fun getComboStock(comboId: Long): Int {
        val combo = comboRepository.findById(comboId)
            ?: throw IllegalArgumentException("Combo not found with id: $comboId")
        if (combo.items.isEmpty()) return 0
        val fromStock = combo.items.minOfOrNull { item ->
            val stock = stockManagementService.getCurrentStock(item.product.id)
            stock / item.quantity
        } ?: 0
        return if (combo.maxAvailableQuantity != null) {
            minOf(combo.maxAvailableQuantity!!, fromStock)
        } else {
            fromStock
        }
    }

    fun previewCostAndPrices(items: List<ComboPreviewItemRequest>): ComboPreviewCostAndPricesResponse {
        if (items.size < 2) {
            return ComboPreviewCostAndPricesResponse(
                items = emptyList(),
                totalCost = BigDecimal.ZERO,
                totalSaleRef = BigDecimal.ZERO,
                suggestedPrice = null,
                comboQuantityAvailable = 0,
                canCreate = false,
                message = "Combo must have at least 2 products",
            )
        }
        val productIds = items.map { it.productId }.toSet()
        if (productIds.size != items.size) {
            return ComboPreviewCostAndPricesResponse(
                items = emptyList(),
                totalCost = BigDecimal.ZERO,
                totalSaleRef = BigDecimal.ZERO,
                suggestedPrice = null,
                comboQuantityAvailable = 0,
                canCreate = false,
                message = "Duplicate products are not allowed",
            )
        }
        val previewItems = items.map { req ->
            val product = productRepository.findById(req.productId)
                ?: throw IllegalArgumentException("Product not found: ${req.productId}")
            val stockQty = try {
                stockManagementService.getCurrentStock(req.productId)
            } catch (_: Exception) {
                0
            }
            val catalog = catalogJpa.findByProduct_Id(req.productId)
            val costPrice = catalog?.costPrice?.takeIf { it > BigDecimal.ZERO }
                ?: stockManagementService.getStockSummary(req.productId).averageCost.takeIf { it > BigDecimal.ZERO }
            val salePriceRef = if (catalog != null && catalog.inCatalog) {
                catalog.finalPrice
            } else {
                try {
                    val cmv = costPrice ?: BigDecimal.ZERO
                    if (cmv <= BigDecimal.ZERO) null
                    else catalogPricingSupport.calculatePrice(req.productId, cmv).calculatedPrice
                } catch (_: Exception) {
                    null
                }
            }
            val hasCost = costPrice != null && costPrice > BigDecimal.ZERO
            val hasSalePrice = salePriceRef != null && salePriceRef >= BigDecimal.ZERO
            val valid = hasCost && hasSalePrice
            ComboPreviewItemResponse(
                productId = req.productId,
                productName = product.name,
                quantity = req.quantity,
                costPrice = costPrice,
                salePriceRef = salePriceRef,
                stockQuantity = stockQty,
                hasCost = hasCost,
                hasSalePrice = hasSalePrice,
                valid = valid,
            )
        }
        val totalCost = previewItems.sumOf { it.costPrice?.multiply(BigDecimal(it.quantity)) ?: BigDecimal.ZERO }
            .setScale(2, RoundingMode.HALF_UP)
        val totalSaleRef = previewItems.sumOf { it.salePriceRef?.multiply(BigDecimal(it.quantity)) ?: BigDecimal.ZERO }
            .setScale(2, RoundingMode.HALF_UP)
        val comboQuantityAvailable = previewItems.map { it.stockQuantity / it.quantity }.minOrNull() ?: 0
        val suggestedPrice = try {
            if (totalCost > BigDecimal.ZERO) {
                catalogPricingSupport.calculatePrice(null, totalCost).calculatedPrice
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
        val canCreate = previewItems.all { it.valid }
        val message = if (!canCreate) {
            val invalid = previewItems.filter { !it.valid }
            invalid.joinToString("; ") { "${it.productName}: missing cost or sale price" }
        } else {
            null
        }
        return ComboPreviewCostAndPricesResponse(
            items = previewItems,
            totalCost = totalCost,
            totalSaleRef = totalSaleRef,
            suggestedPrice = suggestedPrice,
            comboQuantityAvailable = comboQuantityAvailable,
            canCreate = canCreate,
            message = message,
        )
    }

    @Transactional
    fun create(request: ComboCreateRequest, imageFile: MultipartFile? = null): ComboResponse {
        val preview = previewCostAndPrices(request.items.map { ComboPreviewItemRequest(it.productId, it.quantity) })
        if (!preview.canCreate) {
            throw IllegalArgumentException(preview.message ?: "All products must have cost and sale price reference")
        }
        if (request.salePrice < BigDecimal.ZERO) {
            throw IllegalArgumentException("Sale price must be >= 0")
        }
        request.maxAvailableQuantity?.let { max ->
            if (max < 0 || max > preview.comboQuantityAvailable) {
                throw IllegalArgumentException("Quantidade disponível deve ser entre 0 e ${preview.comboQuantityAvailable} (máximo pelo estoque)")
            }
        }
        val category = getComboCategory()
        val brand = getComboBrand()
        val hasUpload = imageFile != null && !imageFile.isEmpty
        val product = Product(
            name = request.name,
            description = request.description,
            category = category,
            brand = brand,
            imageUrl = if (hasUpload) null else request.imageUrl,
            imageData = if (hasUpload) imageFile!!.bytes else null,
            imageMediaType = if (hasUpload) (imageFile!!.contentType?.takeIf { it.isNotBlank() } ?: "image/jpeg") else null,
            displayId = null,
        )
        val savedProduct = productRepository.save(product)
        val combo = Combo(
            product = savedProduct,
            name = request.name,
            description = request.description,
            imageUrl = savedProduct.imageUrl,
            imageData = savedProduct.imageData,
            imageMediaType = savedProduct.imageMediaType,
            salePrice = request.salePrice,
            active = request.active,
            maxAvailableQuantity = request.maxAvailableQuantity,
        )
        val savedCombo = comboRepository.save(combo)
        savedProduct.displayId = savedCombo.id.toInt()
        productRepository.save(savedProduct)
        val finalPrice = computeFinalPrice(request.salePrice, request.discountPercentage)
        val costPrice = preview.totalCost
        val catalog = CatalogEntry(
            product = savedProduct,
            costPrice = costPrice,
            salePrice = request.salePrice,
            discountPercentage = request.discountPercentage,
            finalPrice = finalPrice,
            inCatalog = request.active,
        )
        catalogJpa.save(catalog)
        request.items.forEach { req ->
            val compProduct = productRepository.findById(req.productId)
                ?: throw IllegalArgumentException("Product not found: ${req.productId}")
            val item = ComboItem(combo = savedCombo, product = compProduct, quantity = req.quantity)
            savedCombo.items.add(item)
        }
        comboRepository.save(savedCombo)
        return toDto(savedCombo)
    }

    @Transactional
    fun update(id: Long, request: ComboUpdateRequest, imageFile: MultipartFile? = null): ComboResponse {
        val combo = comboRepository.findByIdWithItems(id)
            ?: comboRepository.findById(id) ?: throw IllegalArgumentException("Combo not found with id: $id")
        val preview = previewCostAndPrices(request.items.map { ComboPreviewItemRequest(it.productId, it.quantity) })
        if (!preview.canCreate) {
            throw IllegalArgumentException(preview.message ?: "All products must have cost and sale price reference")
        }
        if (request.salePrice < BigDecimal.ZERO) {
            throw IllegalArgumentException("Sale price must be >= 0")
        }
        request.maxAvailableQuantity?.let { max ->
            if (max < 0 || max > preview.comboQuantityAvailable) {
                throw IllegalArgumentException("Quantidade disponível deve ser entre 0 e ${preview.comboQuantityAvailable} (máximo pelo estoque)")
            }
        }
        val product = combo.product
        product.name = request.name
        product.description = request.description
        val hasUpload = imageFile != null && !imageFile.isEmpty
        if (hasUpload) {
            product.imageData = imageFile!!.bytes
            product.imageMediaType = imageFile.contentType?.takeIf { it.isNotBlank() } ?: "image/jpeg"
            product.imageUrl = null
        } else {
            product.imageUrl = request.imageUrl
            product.imageData = null
            product.imageMediaType = null
        }
        productRepository.save(product)
        combo.name = request.name
        combo.description = request.description
        combo.imageUrl = product.imageUrl
        combo.imageData = product.imageData
        combo.imageMediaType = product.imageMediaType
        combo.salePrice = request.salePrice
        combo.active = request.active
        combo.maxAvailableQuantity = request.maxAvailableQuantity
        combo.items.clear()
        request.items.forEach { req ->
            val compProduct = productRepository.findById(req.productId)
                ?: throw IllegalArgumentException("Product not found: ${req.productId}")
            combo.items.add(ComboItem(combo = combo, product = compProduct, quantity = req.quantity))
        }
        comboRepository.save(combo)
        val catalog = catalogJpa.findByProduct_Id(product.id)
            ?: throw IllegalStateException("Catalog entry not found for combo product")
        catalog.costPrice = preview.totalCost
        catalog.salePrice = request.salePrice
        catalog.discountPercentage = request.discountPercentage
        catalog.finalPrice = computeFinalPrice(request.salePrice, request.discountPercentage)
        catalog.inCatalog = request.active
        catalogJpa.save(catalog)
        return toDto(combo)
    }

    private fun computeFinalPrice(salePrice: BigDecimal, discountPercentage: BigDecimal?): BigDecimal {
        if (discountPercentage != null && discountPercentage > BigDecimal.ZERO) {
            val discount = discountPercentage.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
            return salePrice.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP)
        }
        return salePrice
    }

    fun findAll(activeOnly: Boolean? = null): List<ComboResponse> {
        val list = if (activeOnly == null) comboRepository.findAllWithItems()
        else comboRepository.findAllByActiveWithItems(activeOnly)
        return list.map { toDto(it) }
    }

    fun getById(id: Long): ComboResponse {
        val combo = comboRepository.findByIdWithItems(id)
            ?: comboRepository.findById(id) ?: throw IllegalArgumentException("Combo not found with id: $id")
        combo.items.size
        return toDto(combo)
    }

    @Transactional
    fun delete(id: Long) {
        val combo = comboRepository.findById(id)
            ?: throw IllegalArgumentException("Combo not found with id: $id")
        combo.active = false
        comboRepository.save(combo)
        val catalog = catalogJpa.findByProduct_Id(combo.product.id) ?: return
        catalog.inCatalog = false
        catalogJpa.save(catalog)
    }

    @Transactional
    fun setActive(id: Long, active: Boolean): ComboResponse {
        val combo = comboRepository.findById(id)
            ?: throw IllegalArgumentException("Combo not found with id: $id")
        combo.active = active
        comboRepository.save(combo)
        val catalog = catalogJpa.findByProduct_Id(combo.product.id) ?: return toDto(combo)
        catalog.inCatalog = active
        catalogJpa.save(catalog)
        return toDto(combo)
    }

    private fun toDto(combo: Combo): ComboResponse {
        val catalog = catalogJpa.findByProduct_Id(combo.product.id)
        val stock = getComboStock(combo.id)
        val catId = combo.product.category.displayId?.let { String.format("%02d", it) }
        val brandId = combo.product.brand.displayId?.let { String.format("%02d", it) }
        val prodId = combo.product.displayId?.let { String.format("%03d", it) }
        val formattedDisplayId = if (catId != null && brandId != null && prodId != null) catId + brandId + prodId else null
        return ComboResponse(
            id = combo.id,
            productId = combo.product.id,
            name = combo.name,
            description = combo.description,
            imageUrl = if (combo.product.imageData != null) "/api/products/${combo.product.id}/image" else combo.product.imageUrl,
            salePrice = combo.salePrice,
            discountPercentage = catalog?.discountPercentage,
            finalPrice = catalog?.finalPrice ?: combo.salePrice,
            costPrice = catalog?.costPrice,
            active = combo.active,
            items = combo.items.map {
                ComboItemResponse(it.id, it.product.id, it.product.name, it.quantity)
            },
            stockQuantity = stock,
            maxAvailableQuantity = combo.maxAvailableQuantity,
            formattedDisplayId = formattedDisplayId,
        )
    }
}
