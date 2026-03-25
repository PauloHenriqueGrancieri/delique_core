package com.delique.core.sales.infrastructure.persistence

import com.delique.core.product.domain.model.Product
import com.delique.core.sales.domain.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ClientJpa : JpaRepository<Client, Long>

interface CustomerOrderJpa : JpaRepository<CustomerOrder, Long> {
    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.sales ORDER BY o.createdAt DESC")
    fun findAllWithSales(): List<CustomerOrder>

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.sales WHERE o.createdAt >= :start AND o.createdAt <= :end ORDER BY o.createdAt DESC")
    fun findByCreatedAtBetweenWithSales(start: LocalDateTime, end: LocalDateTime): List<CustomerOrder>

    @Query("SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.sales WHERE o.id = :id")
    fun findByIdWithSales(id: Long): CustomerOrder?

    @Query("SELECT MIN(o.createdAt) FROM CustomerOrder o")
    fun findFirstOrderDate(): LocalDateTime?
}

interface SaleLineJpa : JpaRepository<SaleLine, Long> {
    fun findByProduct(product: Product): List<SaleLine>

    @Query("SELECT s FROM SaleLine s WHERE s.product = :product AND s.createdAt >= :start AND s.createdAt <= :end")
    fun findByProductAndCreatedAtBetween(product: Product, start: LocalDateTime, end: LocalDateTime): List<SaleLine>

    @Query("SELECT COUNT(DISTINCT s.customerOrder.id) FROM SaleLine s WHERE s.product = :product AND s.createdAt >= :start AND s.createdAt <= :end")
    fun countDistinctOrdersByProductAndCreatedAtBetween(product: Product, start: LocalDateTime, end: LocalDateTime): Long
}

interface SaleReturnJpa : JpaRepository<SaleReturn, Long> {
    fun findByCustomerOrder_Id(orderId: Long): List<SaleReturn>

    @Query("SELECT r FROM SaleReturn r WHERE r.returnedAt >= :start AND r.returnedAt <= :end ORDER BY r.returnedAt DESC")
    fun findByPeriod(start: LocalDateTime, end: LocalDateTime): List<SaleReturn>
}

interface ComboItemJpa : JpaRepository<com.delique.core.inventory.domain.model.ComboItem, Long> {
    fun findByCombo_Id(comboId: Long): List<com.delique.core.inventory.domain.model.ComboItem>
}
