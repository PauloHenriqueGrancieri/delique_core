package com.delique.core.catalog.infrastructure.persistence

import com.delique.core.catalog.domain.model.Catalog
import com.delique.core.catalog.domain.model.CatalogPriceByPaymentMethod
import com.delique.core.catalog.domain.model.CatalogSettings
import com.delique.core.product.domain.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CatalogJpa : JpaRepository<Catalog, Long> {
    fun findByProduct(product: Product): Catalog?
    fun findByProduct_Id(productId: Long): Catalog?

    @Query("SELECT c FROM Catalog c JOIN FETCH c.product p JOIN FETCH p.category JOIN FETCH p.brand")
    fun findAllWithProductCategoryAndBrand(): List<Catalog>
}

interface CatalogSettingsJpa : JpaRepository<CatalogSettings, Long>

interface CatalogPriceByPaymentMethodJpa : JpaRepository<CatalogPriceByPaymentMethod, Long> {
    @Query("SELECT x FROM CatalogPriceByPaymentMethod x WHERE x.product.id = :productId")
    fun findByProductId(@org.springframework.data.repository.query.Param("productId") productId: Long): List<CatalogPriceByPaymentMethod>

    fun deleteByProduct_Id(productId: Long)
}
