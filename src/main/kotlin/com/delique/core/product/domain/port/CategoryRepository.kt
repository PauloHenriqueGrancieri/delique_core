package com.delique.core.product.domain.port

import com.delique.core.product.domain.model.Category

interface CategoryRepository {
    fun findById(id: Long): Category?
    fun findAll(): List<Category>
    fun findByName(name: String): Category?
    fun save(category: Category): Category
    fun delete(id: Long)
    fun existsByName(name: String): Boolean
    fun nextDisplayId(): Int
}
