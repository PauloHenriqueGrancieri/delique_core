package com.delique.core.inventory.domain.port

import com.delique.core.inventory.domain.model.Combo

interface ComboRepository {
    fun save(combo: Combo): Combo
    fun findById(id: Long): Combo?
    fun findByIdWithItems(id: Long): Combo?
    fun findByProductId(productId: Long): Combo?
    fun findByProductIdWithItems(productId: Long): Combo?
    fun findAllWithItems(): List<Combo>
    fun findAllByActiveWithItems(active: Boolean): List<Combo>
}
