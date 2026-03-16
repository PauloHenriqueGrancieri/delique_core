package com.delique.core.financial.application

import com.delique.core.financial.application.dto.CashInflowDto
import com.delique.core.financial.application.dto.CreateCashInflowDto
import com.delique.core.financial.application.dto.InflowEntryDto
import com.delique.core.financial.application.dto.UpdateCashInflowDto
import com.delique.core.financial.domain.model.CashInflow
import com.delique.core.financial.domain.model.CashInflowType
import com.delique.core.financial.infrastructure.persistence.CashInflowJpa
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class CashInflowApplicationService(
    private val cashInflowJpa: CashInflowJpa,
    private val customerOrderJpa: CustomerOrderJpa,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getById(id: Long): CashInflowDto {
        val e = cashInflowJpa.findById(id).orElseThrow {
            log.warn("Cash inflow not found: id=$id")
            IllegalArgumentException("Cash inflow not found: $id")
        }
        return e.toDto()
    }

    fun findByDateBetween(start: LocalDate, end: LocalDate): List<CashInflowDto> =
        cashInflowJpa.findByDateBetweenOrderByDateAsc(start, end).map { it.toDto() }

    fun findCombined(startDate: LocalDate, endDate: LocalDate): List<InflowEntryDto> {
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)
        val manual = cashInflowJpa.findByDateBetweenOrderByDateAsc(startDate, endDate).map {
            InflowEntryDto(
                type = "MANUAL",
                id = it.id,
                date = it.date,
                amount = it.amount,
                description = it.description,
                referenceId = null,
                manualType = it.type.name,
            )
        }
        val orders = customerOrderJpa.findByCreatedAtBetweenWithSales(start, end)
        val sales = orders.map { order ->
            var orderSubtotal = BigDecimal.ZERO
            for (sale in order.sales) {
                val isBrinde = sale.product.category.name.equals("Brinde", ignoreCase = true)
                if (!isBrinde) {
                    val itemTotal = sale.unitPrice.multiply(BigDecimal(sale.quantity)).subtract(sale.discount ?: BigDecimal.ZERO)
                    orderSubtotal = orderSubtotal.add(itemTotal)
                }
            }
            val totalWithDiscount = orderSubtotal.subtract(order.orderDiscountValue ?: BigDecimal.ZERO)
            val orderEntry = totalWithDiscount.subtract(order.feeValue ?: BigDecimal.ZERO)
            InflowEntryDto(
                type = "SALE",
                id = order.id,
                date = order.createdAt.toLocalDate(),
                amount = orderEntry,
                description = null,
                referenceId = order.id,
                manualType = null,
            )
        }
        return (manual + sales).sortedBy { it.date }
    }

    @Transactional
    fun create(dto: CreateCashInflowDto): CashInflowDto {
        val type = try {
            CashInflowType.valueOf(dto.type.uppercase())
        } catch (_: Exception) {
            CashInflowType.OTHER
        }
        val e = CashInflow(
            amount = dto.amount,
            date = dto.date,
            description = dto.description,
            type = type,
        )
        return cashInflowJpa.save(e).toDto()
    }

    @Transactional
    fun update(id: Long, dto: UpdateCashInflowDto): CashInflowDto {
        val e = cashInflowJpa.findById(id).orElseThrow { IllegalArgumentException("Cash inflow not found: $id") }
        dto.amount?.let { e.amount = it }
        dto.date?.let { e.date = it }
        dto.description?.let { e.description = it }
        dto.type?.let {
            e.type = try {
                CashInflowType.valueOf(it.uppercase())
            } catch (_: Exception) {
                CashInflowType.OTHER
            }
        }
        return cashInflowJpa.save(e).toDto()
    }

    @Transactional
    fun delete(id: Long) {
        if (!cashInflowJpa.existsById(id)) {
            log.warn("Cash inflow not found: id=$id")
            throw IllegalArgumentException("Cash inflow not found: $id")
        }
        cashInflowJpa.deleteById(id)
    }

    private fun CashInflow.toDto() = CashInflowDto(
        id = id,
        amount = amount,
        date = date,
        description = description,
        type = type.name,
    )
}
