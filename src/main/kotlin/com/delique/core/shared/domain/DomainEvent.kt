package com.delique.core.shared.domain

import java.time.LocalDateTime

interface DomainEvent {
    val occurredAt: LocalDateTime
}
