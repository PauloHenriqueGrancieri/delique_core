package com.delique.core.inventory.domain.event

import com.delique.core.shared.domain.DomainEvent
import java.math.BigDecimal
import java.time.LocalDateTime

data class StockEntryRecordedEvent(
    val productId: Long,
    val movementId: Long,
    val quantity: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class StockLowEvent(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val minimumStock: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class StockDepletedEvent(
    val productId: Long,
    val productName: String,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
