package com.delique.core.product.infrastructure.persistence

import com.delique.core.product.domain.model.Brand
import com.delique.core.product.domain.model.Category
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import com.delique.core.product.domain.port.BrandRepository
import com.delique.core.product.domain.port.CategoryRepository
import com.delique.core.product.domain.port.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface JpaProductJpa : JpaRepository<Product, Long> {
    fun findAllByOrderByDisplayIdAsc(): List<Product>

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))")
    fun searchByNameOrSku(@Param("q") query: String, pageable: Pageable): Page<Product>

    @Query("SELECT COALESCE(MAX(p.displayId), 0) + 1 FROM Product p")
    fun nextDisplayId(): Int
}

@Repository
class ProductRepositoryAdapter(private val jpa: JpaProductJpa) : ProductRepository {
    override fun findById(id: Long)               = jpa.findById(id).orElse(null)
    override fun findAll()                        = jpa.findAll()
    override fun findAllOrdered()                 = jpa.findAllByOrderByDisplayIdAsc()
    override fun search(query: String, p: Pageable) = jpa.searchByNameOrSku(query, p)
    override fun save(product: Product)           = jpa.save(product)
    override fun delete(id: Long)                 = jpa.deleteById(id)
    override fun nextDisplayId()                  = jpa.nextDisplayId()
    override fun existsById(id: Long)             = jpa.existsById(id)
}

interface JpaBrandJpa : JpaRepository<Brand, Long> {
    fun findAllByOrderByDisplayIdAsc(): List<Brand>
    fun findByName(name: String): Brand?
    @Query("SELECT COALESCE(MAX(b.displayId), 0) + 1 FROM Brand b")
    fun nextDisplayId(): Int
}

@Repository
class BrandRepositoryAdapter(private val jpa: JpaBrandJpa) : BrandRepository {
    override fun findById(id: Long)        = jpa.findById(id).orElse(null)
    override fun findAll()                 = jpa.findAll()
    override fun findAllOrdered()          = jpa.findAllByOrderByDisplayIdAsc()
    override fun findByName(name: String)  = jpa.findByName(name)
    override fun save(brand: Brand)        = jpa.save(brand)
    override fun delete(id: Long)          = jpa.deleteById(id)
    override fun nextDisplayId()           = jpa.nextDisplayId()
}

interface JpaCategoryJpa : JpaRepository<Category, Long> {
    fun findByName(name: String): Category?
    fun existsByName(name: String): Boolean
}

@Repository
class CategoryRepositoryAdapter(private val jpa: JpaCategoryJpa) : CategoryRepository {
    override fun findById(id: Long)           = jpa.findById(id).orElse(null)
    override fun findAll()                    = jpa.findAll()
    override fun findByName(name: String)     = jpa.findByName(name)
    override fun save(category: Category)     = jpa.save(category)
    override fun delete(id: Long)             = jpa.deleteById(id)
    override fun existsByName(name: String)   = jpa.existsByName(name)
}

interface JpaVariationOptionJpa : JpaRepository<ProductVariationOption, Long>
