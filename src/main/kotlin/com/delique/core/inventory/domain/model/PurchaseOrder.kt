package com.delique.core.inventory.domain.model

import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.model.ProductVariationOption
import com.delique.core.supplier.domain.model.Supplier
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class PurchaseOrderStatus {
    PENDING,
    DELIVERED,
    CANCELLED,
}

@Entity
@Table(name = "purchase_orders")
class PurchaseOrder(
    @Column(name = "total_freight", precision = 19, scale = 2, nullable = false)
    var totalFreight: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PurchaseOrderStatus = PurchaseOrderStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    var supplier: Supplier? = null,

    @OneToMany(mappedBy = "purchaseOrder", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<PurchaseOrderItem> = mutableListOf(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}

@Entity
@Table(name = "purchase_order_items")
class PurchaseOrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    var purchaseOrder: PurchaseOrder,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "unit_cost", precision = 19, scale = 2, nullable = false)
    var unitCost: BigDecimal,

    @Column(name = "received_quantity")
    var receivedQuantity: Int? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDate? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variation_option_id")
    var variationOption: ProductVariationOption? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}
