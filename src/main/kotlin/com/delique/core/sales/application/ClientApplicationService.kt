package com.delique.core.sales.application

import com.delique.core.sales.application.dto.ClientDto
import com.delique.core.sales.domain.model.Client
import com.delique.core.sales.infrastructure.persistence.ClientJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClientApplicationService(
    private val clientJpa: ClientJpa,
) {
    fun getAllClients(): List<ClientDto> =
        clientJpa.findAll().map { it.toDto() }

    fun getClientById(id: Long): ClientDto {
        val c = clientJpa.findById(id).orElseThrow { IllegalArgumentException("Client not found with id: $id") }
        return c.toDto()
    }

    fun getClientEntity(id: Long): Client =
        clientJpa.findById(id).orElseThrow { IllegalArgumentException("Client not found with id: $id") }

    @Transactional
    fun createClient(dto: ClientDto): ClientDto {
        val c = Client(
            name = dto.name,
            phone = dto.phone,
            age = dto.age,
            location = dto.location,
            canal = dto.canal,
            interests = dto.interests,
        )
        return clientJpa.save(c).toDto()
    }

    @Transactional
    fun updateClient(id: Long, dto: ClientDto): ClientDto {
        val c = clientJpa.findById(id).orElseThrow { IllegalArgumentException("Client not found with id: $id") }
        c.name = dto.name
        c.phone = dto.phone
        c.age = dto.age
        c.location = dto.location
        c.canal = dto.canal
        c.interests = dto.interests
        return clientJpa.save(c).toDto()
    }

    @Transactional
    fun deleteClient(id: Long) {
        clientJpa.deleteById(id)
    }

    private fun Client.toDto() = ClientDto(
        id = id,
        name = name,
        phone = phone,
        age = age,
        location = location,
        canal = canal,
        interests = interests,
    )
}
