package com.delique.core.supplier.domain.port

import com.delique.core.supplier.domain.model.ProductSupplier

interface ProductSupplierRepository {
    fun findById(id: Long): ProductSupplier?
    fun findByProductId(productId: Long): List<ProductSupplier>
    fun findBySupplierId(supplierId: Long): List<ProductSupplier>
    fun findByProductIdAndSupplierId(productId: Long, supplierId: Long): ProductSupplier?
    /** Product-supplier links with a URL and supplier not excluded from scraping. */
    fun findAllScrapeable(): List<ProductSupplier>
    fun save(productSupplier: ProductSupplier): ProductSupplier
    fun delete(id: Long)
}
