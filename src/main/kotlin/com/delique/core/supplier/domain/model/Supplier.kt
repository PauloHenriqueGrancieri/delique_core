package com.delique.core.supplier.domain.model

import com.delique.core.shared.infrastructure.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "suppliers")
class Supplier(
    @Column(nullable = false)
    var name: String,

    @Column
    var website: String? = null,

    @ElementCollection
    @CollectionTable(name = "supplier_emails", joinColumns = [JoinColumn(name = "supplier_id")])
    @Column(name = "email")
    var emails: MutableList<String> = mutableListOf(),

    @ElementCollection
    @CollectionTable(name = "supplier_phones", joinColumns = [JoinColumn(name = "supplier_id")])
    @Column(name = "phone")
    var phones: MutableList<String> = mutableListOf(),

    @Column(precision = 19, scale = 2)
    var freight: BigDecimal = BigDecimal.ZERO,

    @Column(name = "min_free_freight", precision = 19, scale = 2)
    var minFreeFreight: BigDecimal = BigDecimal.ZERO,

    @Column(name = "min_order_value", precision = 19, scale = 2)
    var minOrderValue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "scraper_excluded", nullable = false)
    var scraperExcluded: Boolean = false,

    @Column(name = "scraper_success_selectors", columnDefinition = "TEXT")
    var scraperSuccessSelectors: String? = null,
) : BaseEntity() {

    fun isFreightFree(orderValue: BigDecimal): Boolean =
        minFreeFreight > BigDecimal.ZERO && orderValue >= minFreeFreight

    fun effectiveFreight(orderValue: BigDecimal): BigDecimal =
        if (isFreightFree(orderValue)) BigDecimal.ZERO else freight
}
