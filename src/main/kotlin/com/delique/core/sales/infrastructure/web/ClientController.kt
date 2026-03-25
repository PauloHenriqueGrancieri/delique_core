package com.delique.core.sales.infrastructure.web

import com.delique.core.sales.application.ClientApplicationService
import com.delique.core.sales.application.dto.ClientDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = ["http://localhost:3000"])
class ClientController(
    private val clientApplicationService: ClientApplicationService,
) {
    @GetMapping
    fun getAllClients(): ResponseEntity<List<ClientDto>> =
        ResponseEntity.ok(clientApplicationService.getAllClients())

    @GetMapping("/{id}")
    fun getClientById(@PathVariable id: Long): ResponseEntity<ClientDto> =
        ResponseEntity.ok(clientApplicationService.getClientById(id))

    @PostMapping
    fun createClient(@Valid @RequestBody dto: ClientDto): ResponseEntity<ClientDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(clientApplicationService.createClient(dto))

    @PutMapping("/{id}")
    fun updateClient(@PathVariable id: Long, @Valid @RequestBody dto: ClientDto): ResponseEntity<ClientDto> =
        ResponseEntity.ok(clientApplicationService.updateClient(id, dto))

    @DeleteMapping("/{id}")
    fun deleteClient(@PathVariable id: Long): ResponseEntity<Void> {
        clientApplicationService.deleteClient(id)
        return ResponseEntity.noContent().build()
    }
}
