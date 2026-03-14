package com.delique.core.sales.application

import com.delique.core.financial.domain.model.CashInflow
import com.delique.core.financial.domain.model.CashInflowType
import com.delique.core.financial.infrastructure.persistence.CashInflowJpa
import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.inventory.domain.model.StockMovement
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.sales.application.dto.SaleReturnDto
import com.delique.core.sales.application.dto.SaleReturnItemDto
import com.delique.core.sales.application.dto.SaleReturnRequest
import com.delique.core.sales.domain.model.SaleReturn
import com.delique.core.sales.domain.model.SaleReturnLine
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import com.delique.core.sales.infrastructure.persistence.SaleReturnJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SaleReturnApplicationService(
    private val saleReturnJpa: SaleReturnJpa,
    private val customerOrderJpa: CustomerOrderJpa,
    private val productRepository: ProductRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val cashInflowJpa: CashInflowJpa,
) {
    @Transactional
    fun processReturn(orderId: Long, request: SaleReturnRequest): SaleReturnDto {
        val order = customerOrderJpa.findByIdWithSales(orderId)
            ?: throw IllegalArgumentException("Pedido não encontrado: $orderId")

        if (request.items.isEmpty()) {
            throw IllegalArgumentException("A devolução deve conter pelo menos um item")
        }

        val soldQty = order.sales.groupBy { it.product.id }
            .mapValues { (_, sales) -> sales.sumOf { it.quantity } }

        val existingReturns = saleReturnJpa.findByCustomerOrder_Id(orderId)
        val alreadyReturned = existingReturns.flatMap { it.items }
            .groupBy { it.product.id }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }

        for (itemReq in request.items) {
            val sold = soldQty[itemReq.productId] ?: 0
            val returned = alreadyReturned[itemReq.productId] ?: 0
            val available = sold - returned
            if (itemReq.quantity > available) {
                throw IllegalArgumentException(
                    "Produto ${itemReq.productId}: quantidade a devolver (${itemReq.quantity}) excede o disponível para devolução ($available)",
                )
            }
        }

        val saleReturn = SaleReturn(customerOrder = order, reason = request.reason)
        val savedReturn = saleReturnJpa.save(saleReturn)

        var totalRefunded = BigDecimal.ZERO

        for (itemReq in request.items) {
            val product = productRepository.findById(itemReq.productId)
                ?: throw IllegalArgumentException("Produto não encontrado: ${itemReq.productId}")

            val returnLine = SaleReturnLine(
                saleReturn = savedReturn,
                product = product,
                quantity = itemReq.quantity,
                unitPrice = itemReq.unitPrice,
                variationOption = itemReq.variationOption,
            )
            savedReturn.items.add(returnLine)

            stockMovementRepository.save(
                StockMovement(
                    product = product,
                    quantity = itemReq.quantity,
                    type = MovementType.RETURN,
                    details = "Devolução #${savedReturn.id} - Pedido #$orderId",
                ),
            )

            totalRefunded = totalRefunded.add(itemReq.unitPrice.multiply(BigDecimal(itemReq.quantity)))
        }

        saleReturnJpa.save(savedReturn)

        cashInflowJpa.save(
            CashInflow(
                amount = totalRefunded.negate(),
                date = LocalDate.now(),
                description = "Devolução #${savedReturn.id} - Pedido #$orderId",
                type = CashInflowType.OTHER,
            ),
        )

        return savedReturn.toDto(totalRefunded)
    }

    @Transactional(readOnly = true)
    fun getReturnsByOrder(orderId: Long): List<SaleReturnDto> =
        saleReturnJpa.findByCustomerOrder_Id(orderId).map { it.toDto() }

    @Transactional(readOnly = true)
    fun getAllReturns(startDate: LocalDateTime?, endDate: LocalDateTime?): List<SaleReturnDto> {
        val start = startDate ?: LocalDateTime.of(2000, 1, 1, 0, 0)
        val end = endDate ?: LocalDateTime.now()
        return saleReturnJpa.findByPeriod(start, end).map { it.toDto() }
    }

    private fun SaleReturn.toDto(total: BigDecimal? = null): SaleReturnDto {
        val itemDtos = this.items.map { item ->
            SaleReturnItemDto(
                id = item.id,
                productId = item.product.id,
                productName = item.product.name,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                variationOption = item.variationOption,
            )
        }
        val totalRefunded = total ?: itemDtos.fold(BigDecimal.ZERO) { acc, i ->
            acc.add(i.unitPrice.multiply(BigDecimal(i.quantity)))
        }
        return SaleReturnDto(
            id = this.id,
            orderId = this.customerOrder.id,
            returnedAt = this.returnedAt,
            reason = this.reason,
            items = itemDtos,
            totalRefunded = totalRefunded,
        )
    }
}
