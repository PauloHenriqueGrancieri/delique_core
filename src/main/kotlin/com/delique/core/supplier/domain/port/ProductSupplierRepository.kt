package com.delique.core.supplier.domain.port

import com.delique.core.supplier.domain.model.ProductSupplier

interface ProductSupplierRepository {
    fun findByProductId(productId: Long): List<ProductSupplier>
    fun findBySupplierId(supplierId: Long): List<ProductSupplier>
    fun findByProductIdAndSupplierId(productId: Long, supplierId: Long): ProductSupplier?
    fun save(productSupplier: ProductSupplier): ProductSupplier
    fun delete(id: Long)
}
