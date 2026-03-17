package com.delique.core.inventory.application

import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.PurchaseOrderStatus
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.port.PurchaseOrderRepository
import com.delique.core.inventory.domain.port.StockMovementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

data class StockReconciliationResult(
    val movementsUpdated: Int,
    val message: String,
)

@Service
class StockReconciliationService(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val stockMovementRepository: StockMovementRepository,
) {
    @Transactional
    fun reconcile(): StockReconciliationResult {
        var movementsUpdated = 0
        val deliveredPos = purchaseOrderRepository.findByStatusWithItems(PurchaseOrderStatus.DELIVERED)

        for (po in deliveredPos) {
            val receivedItems = po.items.filter { it.receivedQuantity != null && it.receivedQuantity!! > 0 }
            if (receivedItems.isEmpty()) continue

            val totalReceivedValue = receivedItems.fold(BigDecimal.ZERO) { acc, item ->
                val qty = item.receivedQuantity!!.toBigDecimal()
                acc.add(item.unitCost.multiply(qty))
            }
            val freight = po.totalFreight

            val detailsPrefix = "PO #${po.id}"
            val movements = stockMovementRepository.findByTypeAndDetails(MovementType.ENTRY, detailsPrefix)
            if (movements.isEmpty()) continue

            val itemsWithPrices = receivedItems.map { item ->
                val receivedQty = item.receivedQuantity!!.toBigDecimal()
                val itemValue = item.unitCost.multiply(receivedQty)
                val freightShare = if (totalReceivedValue.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
                else freight.multiply(itemValue).divide(totalReceivedValue, 2, RoundingMode.HALF_UP)
                val price = item.unitCost.add(freightShare.divide(receivedQty, 2, RoundingMode.HALF_UP))
                item to price
            }.sortedWith(
                compareBy(
                    { it.first.product.id },
                    { it.first.variationOption?.id ?: -1L },
                    { it.first.receivedQuantity!! },
                    { it.first.id },
                ),
            )

            val sortedMovements = movements.sortedWith(
                compareBy<StockMovement> { it.product.id }
                    .thenBy { it.variationOption?.id ?: -1L }
                    .thenBy { it.quantity }
                    .thenBy { it.id },
            )

            if (itemsWithPrices.size != sortedMovements.size) continue

            for (i in sortedMovements.indices) {
                if (i >= itemsWithPrices.size) break
                val (item, newPrice) = itemsWithPrices[i]
                val movement = sortedMovements[i]
                if (movement.product.id != item.product.id || movement.quantity != item.receivedQuantity!!) continue
                if (movement.variationOption?.id != item.variationOption?.id) continue
                if (movement.purchasePrice != newPrice) {
                    movement.purchasePrice = newPrice
                    stockMovementRepository.save(movement)
                    movementsUpdated++
                }
            }
        }

        return StockReconciliationResult(
            movementsUpdated = movementsUpdated,
            message = "Reconciliação concluída. $movementsUpdated movimento(s) de estoque atualizado(s) com base nos pedidos de compra.",
        )
    }
}
