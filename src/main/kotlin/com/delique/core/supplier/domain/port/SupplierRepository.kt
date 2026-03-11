package com.delique.core.supplier.domain.port

import com.delique.core.supplier.domain.model.Supplier

interface SupplierRepository {
    fun findById(id: Long): Supplier?
    fun findAll(): List<Supplier>
    fun findAllActive(): List<Supplier>
    fun save(supplier: Supplier): Supplier
    fun delete(id: Long)
    fun existsById(id: Long): Boolean
}
