package com.delique.core.inventory.domain.port

import com.delique.core.inventory.domain.model.PurchaseOrder
import com.delique.core.inventory.domain.model.PurchaseOrderStatus

interface PurchaseOrderRepository {
    fun save(order: PurchaseOrder): PurchaseOrder
    fun findById(id: Long): PurchaseOrder?
    fun findByIdWithItems(id: Long): PurchaseOrder?
    fun findByStatusWithItems(status: PurchaseOrderStatus): List<PurchaseOrder>
    fun findAllWithItemsDesc(): List<PurchaseOrder>
    fun delete(order: PurchaseOrder)
}
