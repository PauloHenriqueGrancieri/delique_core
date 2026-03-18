package com.delique.core.product.domain.port

import com.delique.core.product.domain.model.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun findById(id: Long): Product?
    fun findAll(): List<Product>
    fun findAllOrdered(): List<Product>
    fun search(query: String, pageable: Pageable): Page<Product>
    fun save(product: Product): Product
    fun delete(id: Long)
    fun nextDisplayId(): Int
    fun existsById(id: Long): Boolean
    fun findByMinimumStockIsNotNull(): List<Product>
}
