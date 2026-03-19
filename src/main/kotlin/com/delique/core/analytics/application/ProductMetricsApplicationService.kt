package com.delique.core.analytics.application

import com.delique.core.analytics.domain.model.PeriodType
import com.delique.core.analytics.domain.model.ProductMetrics
import com.delique.core.analytics.infrastructure.persistence.ProductMetricsJpa
import com.delique.core.inventory.application.StockManagementService
import com.delique.core.inventory.domain.model.MovementType
import com.delique.core.pricing.domain.model.PriceCalculationConfig
import com.delique.core.pricing.infrastructure.persistence.PriceCalculationConfigJpa
import com.delique.core.product.domain.model.Product
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import com.delique.core.sales.infrastructure.persistence.SaleLineJpa
import com.delique.core.inventory.domain.port.StockMovementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class ProductMetricsApplicationService(
    private val productMetricsJpa: ProductMetricsJpa,
    private val saleLineJpa: SaleLineJpa,
    private val customerOrderJpa: CustomerOrderJpa,
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository,
    private val priceCalculationConfigJpa: PriceCalculationConfigJpa,
    private val stockManagementService: StockManagementService,
) {

    fun calculateMetricsForProduct(
        productId: Long,
        periodType: PeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): ProductMetrics {
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)

        val sales = saleLineJpa.findByProductAndCreatedAtBetween(product, start, end)

        if (sales.isEmpty()) {
            return createEmptyMetrics(product, periodType, startDate, endDate)
        }

        val quantidadeVendida = sales.sumOf { it.quantity }
        val numeroPedidos = saleLineJpa.countDistinctOrdersByProductAndCreatedAtBetween(product, start, end).toInt()

        val totalRevenue = sales.sumOf { sale ->
            val itemTotal = sale.unitPrice.multiply(BigDecimal(sale.quantity))
            itemTotal.subtract(sale.discount ?: BigDecimal.ZERO)
        }
        val precoVenda = if (quantidadeVendida > 0) {
            totalRevenue.divide(BigDecimal(quantidadeVendida), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val stockSummary = try {
            stockManagementService.getStockSummary(productId)
        } catch (_: Exception) {
            null
        }
        val cmvAj = stockSummary?.averageCost ?: BigDecimal.ZERO

        val config = priceCalculationConfigJpa.findFirstByOrderByIdAsc()
            ?: PriceCalculationConfig()

        val cvValor = calculateCVValor(config)
        val custosPercentuais = calculateCustosPercentuais(config)

        val lucroUnitario = precoVenda
            .subtract(cmvAj.add(cvValor))
            .subtract(precoVenda.multiply(custosPercentuais.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
            .setScale(2, RoundingMode.HALF_UP)

        val margemLucroPercent = if (precoVenda > BigDecimal.ZERO) {
            lucroUnitario.divide(precoVenda, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val faturamentoTotal = totalRevenue.setScale(2, RoundingMode.HALF_UP)
        val margemTotal = lucroUnitario.multiply(BigDecimal(quantidadeVendida))
            .setScale(2, RoundingMode.HALF_UP)

        val (estoqueMedio, diasComEstoque) = calculateStockMetrics(product, startDate, endDate)

        val existing = productMetricsJpa.findByProductAndPeriodType(product, periodType)
        val metrics = existing ?: ProductMetrics(
            product = product,
            periodStart = startDate,
            periodEnd = endDate,
            periodType = periodType,
        )

        metrics.precoVenda = precoVenda
        metrics.cmvAj = cmvAj
        metrics.margemLucroPercent = margemLucroPercent
        metrics.lucroUnitario = lucroUnitario
        metrics.quantidadeVendida = quantidadeVendida
        metrics.numeroPedidos = numeroPedidos
        metrics.diasComEstoque = diasComEstoque
        metrics.estoqueMedio = estoqueMedio
        metrics.faturamentoTotal = faturamentoTotal
        metrics.margemTotal = margemTotal
        metrics.updatedAt = LocalDateTime.now()

        return productMetricsJpa.save(metrics)
    }

    private fun calculateCVValor(config: PriceCalculationConfig): BigDecimal {
        val totalOrders = customerOrderJpa.count()
        if (totalOrders == 0L) {
            return BigDecimal.ZERO
        }
        val totalItems = saleLineJpa.count()
        if (totalItems == 0L) {
            return BigDecimal.ZERO
        }
        val averageItemsPerOrder = BigDecimal(totalItems).divide(BigDecimal(totalOrders), 2, RoundingMode.HALF_UP)
        if (averageItemsPerOrder <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        val packagingPlusDelivery = config.defaultPackagingValue.add(config.defaultDeliveryValue)
        return packagingPlusDelivery.divide(averageItemsPerOrder, 4, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun calculateCustosPercentuais(config: PriceCalculationConfig): BigDecimal {
        val cvPercent = config.defaultSalesCommissionPercentage
            .add(config.defaultCardFeePercentage)
            .add(config.defaultTaxPercentage)
        val dfPercent = config.defaultFixedExpensePercentage
        return cvPercent.add(dfPercent)
    }

    private fun calculateStockMetrics(product: Product, startDate: LocalDate, endDate: LocalDate): Pair<BigDecimal, Int> {
        val start = startDate.atStartOfDay()
        val end = endDate.atTime(LocalTime.MAX)

        val movements = stockMovementRepository.findByProduct(product)
            .filter { it.createdAt >= start && it.createdAt <= end }
            .sortedBy { it.createdAt }

        if (movements.isEmpty()) {
            return BigDecimal.ZERO to 0
        }

        var currentStock = 0
        val stockByDate = mutableMapOf<LocalDate, Int>()
        for (movement in movements) {
            val movementDate = movement.createdAt.toLocalDate()
            when (movement.type) {
                MovementType.ENTRY -> currentStock += movement.quantity
                MovementType.RETURN -> currentStock += movement.quantity
                MovementType.SALE -> currentStock -= movement.quantity
                MovementType.DELETE -> currentStock -= movement.quantity
            }
            stockByDate[movementDate] = currentStock
        }

        var totalStock = BigDecimal.ZERO
        var daysCount = 0
        var daysWithStockCount = 0

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val stockOnDate = stockByDate.entries
                .filter { it.key <= currentDate }
                .maxByOrNull { it.key }
                ?.value ?: 0

            totalStock = totalStock.add(BigDecimal(stockOnDate))
            daysCount++
            if (stockOnDate > 0) {
                daysWithStockCount++
            }
            currentDate = currentDate.plusDays(1)
        }

        val estoqueMedio = if (daysCount > 0) {
            totalStock.divide(BigDecimal(daysCount), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return estoqueMedio to daysWithStockCount
    }

    private fun createEmptyMetrics(
        product: Product,
        periodType: PeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): ProductMetrics {
        val existing = productMetricsJpa.findByProductAndPeriodType(product, periodType)
        return if (existing != null) {
            existing.updatedAt = LocalDateTime.now()
            productMetricsJpa.save(existing)
        } else {
            val metrics = ProductMetrics(
                product = product,
                periodStart = startDate,
                periodEnd = endDate,
                periodType = periodType,
            )
            productMetricsJpa.save(metrics)
        }
    }

    fun calculateMetricsForAllProducts(periodType: PeriodType): List<ProductMetrics> {
        val products = productRepository.findAll()
        val endDate = LocalDate.now()
        val startDate = when (periodType) {
            PeriodType.LAST_30_DAYS -> endDate.minusDays(30)
            PeriodType.LAST_60_DAYS -> endDate.minusDays(60)
            PeriodType.LAST_90_DAYS -> endDate.minusDays(90)
            PeriodType.LAST_6_MONTHS -> endDate.minusMonths(6)
            PeriodType.LAST_1_YEAR -> endDate.minusYears(1)
        }

        return products.map { product ->
            calculateMetricsForProduct(product.id, periodType, startDate, endDate)
        }
    }

    fun getMetricsForProduct(productId: Long, periodType: PeriodType): ProductMetrics? {
        val product = productRepository.findById(productId) ?: return null
        return productMetricsJpa.findByProductAndPeriodType(product, periodType)
    }
}
