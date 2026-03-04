package com.delique.core.shared.domain

import jakarta.persistence.Transient

abstract class AggregateRoot {

    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()

    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun pullEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }

    fun hasEvents(): Boolean = domainEvents.isNotEmpty()
}
