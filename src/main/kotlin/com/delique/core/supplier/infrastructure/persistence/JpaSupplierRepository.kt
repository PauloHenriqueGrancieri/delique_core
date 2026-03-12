package com.delique.core.supplier.infrastructure.persistence

import com.delique.core.supplier.domain.model.ProductSupplier
import com.delique.core.supplier.domain.model.Supplier
import com.delique.core.supplier.domain.port.ProductSupplierRepository
import com.delique.core.supplier.domain.port.SupplierRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface JpaSupplierJpa : JpaRepository<Supplier, Long> {
    fun findAllByScraperExcludedFalse(): List<Supplier>
}

@Repository
class SupplierRepositoryAdapter(private val jpa: JpaSupplierJpa) : SupplierRepository {
    override fun findById(id: Long)      = jpa.findById(id).orElse(null)
    override fun findAll()               = jpa.findAll()
    override fun findAllActive()         = jpa.findAllByScraperExcludedFalse()
    override fun save(supplier: Supplier) = jpa.save(supplier)
    override fun delete(id: Long)        = jpa.deleteById(id)
    override fun existsById(id: Long)    = jpa.existsById(id)
}

interface JpaProductSupplierJpa : JpaRepository<ProductSupplier, Long> {
    fun findByProductId(productId: Long): List<ProductSupplier>
    fun findBySupplierId(supplierId: Long): List<ProductSupplier>
    fun findByProductIdAndSupplierId(productId: Long, supplierId: Long): ProductSupplier?
}

@Repository
class ProductSupplierRepositoryAdapter(private val jpa: JpaProductSupplierJpa) : ProductSupplierRepository {
    override fun findByProductId(productId: Long)                             = jpa.findByProductId(productId)
    override fun findBySupplierId(supplierId: Long)                           = jpa.findBySupplierId(supplierId)
    override fun findByProductIdAndSupplierId(productId: Long, supplierId: Long) =
        jpa.findByProductIdAndSupplierId(productId, supplierId)
    override fun save(productSupplier: ProductSupplier)                       = jpa.save(productSupplier)
    override fun delete(id: Long)                                             = jpa.deleteById(id)
}
