package com.delique.core.product.domain.model

import com.delique.core.shared.infrastructure.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    @Column(nullable = false, unique = true)
    var name: String,

    @Column(name = "has_validity", nullable = false)
    var hasValidity: Boolean = false,

    @Column(name = "display_id")
    var displayId: Int? = null,
) : BaseEntity()
