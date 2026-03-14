package com.delique.core.inventory.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "stock_unit")
class StockUnit(
    @Id
    @Column(length = 36)
    val id: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_movement_id", nullable = false)
    var stockMovement: StockMovement,

    @Column(name = "unit_index", nullable = false)
    val unitIndex: Int,
)

@Entity
@Table(name = "stock_unit_expiry")
class StockUnitExpiry(
    @EmbeddedId
    val id: StockUnitExpiryId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stockMovementId")
    @JoinColumn(name = "stock_movement_id")
    var stockMovement: StockMovement,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: java.time.LocalDate,
)

@Embeddable
data class StockUnitExpiryId(
    @Column(name = "stock_movement_id") val stockMovementId: Long = 0,
    @Column(name = "unit_index")        val unitIndex: Int = 0,
) : java.io.Serializable
