package com.delique.core.supplier.domain.model

import com.delique.core.product.domain.model.Product
import com.delique.core.shared.infrastructure.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_suppliers")
class ProductSupplier(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    var supplier: Supplier,

    @Column(columnDefinition = "TEXT")
    var url: String? = null,

    @Column(precision = 19, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "out_of_stock_at_supplier", nullable = false)
    var outOfStockAtSupplier: Boolean = false,
) : BaseEntity()
