package com.delique.core.shared.infrastructure

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun <T> of(page: Page<T>): PageResponse<T> = PageResponse(
            content       = page.content,
            page          = page.number,
            size          = page.size,
            totalElements = page.totalElements,
            totalPages    = page.totalPages,
            last          = page.isLast,
        )
    }
}
