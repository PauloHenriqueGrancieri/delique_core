package com.delique.core.product.domain.port

import com.delique.core.product.domain.model.Brand

interface BrandRepository {
    fun findById(id: Long): Brand?
    fun findAll(): List<Brand>
    fun findAllOrdered(): List<Brand>
    fun findByName(name: String): Brand?
    fun save(brand: Brand): Brand
    fun delete(id: Long)
    fun nextDisplayId(): Int
}
