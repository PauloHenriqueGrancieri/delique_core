package com.delique.core.inventory.domain.command

import com.delique.core.inventory.domain.model.StockMovement
import java.math.BigDecimal
import java.time.LocalDate

sealed class StockCommand {
    data class Entry(
        val productId: Long,
        val variationOptionId: Long?,
        val quantity: Int,
        val cost: BigDecimal?,
        val expiresAt: LocalDate?,
        val purchaseOrderId: Long?,
        val details: String?,
    ) : StockCommand()

    data class Sale(
        val saleId: Long,
        val productId: Long,
        val variationOptionId: Long?,
        val quantity: Int,
    ) : StockCommand()

    data class Return(
        val saleId: Long,
        val productId: Long,
        val quantity: Int,
    ) : StockCommand()

    data class Delete(
        val productId: Long,
        val quantity: Int,
        val reason: String?,
    ) : StockCommand()
}

fun interface StockCommandHandler {
    fun handle(command: StockCommand): StockMovement
}
