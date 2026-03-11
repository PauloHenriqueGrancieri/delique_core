package com.delique.core.supplier.domain.service

import com.delique.core.supplier.domain.model.Supplier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class FreightCalculationService {

    fun calculatePerProduct(supplier: Supplier, orderValue: BigDecimal, itemCount: Int): BigDecimal {
        if (itemCount <= 0) return BigDecimal.ZERO
        val freight = supplier.effectiveFreight(orderValue)
        return freight.divide(BigDecimal(itemCount), 2, RoundingMode.HALF_UP)
    }

    fun calculateTotal(supplier: Supplier, orderValue: BigDecimal): BigDecimal =
        supplier.effectiveFreight(orderValue)
}
