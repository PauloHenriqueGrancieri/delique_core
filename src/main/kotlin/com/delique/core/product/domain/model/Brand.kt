package com.delique.core.product.domain.model

import com.delique.core.shared.infrastructure.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "brands")
class Brand(
    @Column(nullable = false, unique = true)
    var name: String,

    @Column(name = "display_id")
    var displayId: Int? = null,
) : BaseEntity()
