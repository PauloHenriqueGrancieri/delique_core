package com.delique.core.analytics.domain.model

import com.delique.core.pricing.domain.model.ABCClass
import com.delique.core.pricing.domain.model.XYZClass
import com.delique.core.product.domain.model.Product
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class PeriodType {
    LAST_30_DAYS,
    LAST_60_DAYS,
    LAST_90_DAYS,
    LAST_6_MONTHS,
    LAST_1_YEAR,
}

@Entity
@Table(name = "product_metrics", uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "period_type"])])
class ProductMetrics(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) var product: Product,
    @Column(name = "period_start", nullable = false) var periodStart: LocalDate,
    @Column(name = "period_end", nullable = false) var periodEnd: LocalDate,
    @Column(name = "period_type", nullable = false) @Enumerated(EnumType.STRING) var periodType: PeriodType,
    @Column(name = "preco_venda", precision = 19, scale = 2) var precoVenda: BigDecimal = BigDecimal.ZERO,
    @Column(name = "cmv_aj", precision = 19, scale = 2) var cmvAj: BigDecimal = BigDecimal.ZERO,
    @Column(name = "margem_lucro_percent", precision = 5, scale = 2) var margemLucroPercent: BigDecimal = BigDecimal.ZERO,
    @Column(name = "lucro_unitario", precision = 19, scale = 2) var lucroUnitario: BigDecimal = BigDecimal.ZERO,
    @Column(name = "quantidade_vendida", nullable = false) var quantidadeVendida: Int = 0,
    @Column(name = "numero_pedidos", nullable = false) var numeroPedidos: Int = 0,
    @Column(name = "dias_com_estoque", nullable = false) var diasComEstoque: Int = 0,
    @Column(name = "estoque_medio", precision = 10, scale = 2) var estoqueMedio: BigDecimal = BigDecimal.ZERO,
    @Column(name = "faturamento_total", precision = 19, scale = 2) var faturamentoTotal: BigDecimal = BigDecimal.ZERO,
    @Column(name = "margem_total", precision = 19, scale = 2) var margemTotal: BigDecimal = BigDecimal.ZERO,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

@Entity
@Table(name = "product_classifications", uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "period_type"])])
class ProductClassification(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) var product: Product,
    @Column(name = "period_start", nullable = false) var periodStart: LocalDate,
    @Column(name = "period_end", nullable = false) var periodEnd: LocalDate,
    @Column(name = "period_type", nullable = false) @Enumerated(EnumType.STRING) var periodType: PeriodType,
    @Column(name = "abc_faturamento", length = 1) @Enumerated(EnumType.STRING) var abcFaturamento: ABCClass? = null,
    @Column(name = "abc_margem", length = 1) @Enumerated(EnumType.STRING) var abcMargem: ABCClass? = null,
    @Column(name = "xyz_giro", length = 1) @Enumerated(EnumType.STRING) var xyzGiro: XYZClass? = null,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
