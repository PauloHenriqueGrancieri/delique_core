package com.delique.core.supplier.infrastructure.web

import com.delique.core.supplier.application.dto.SupplierRequest
import com.delique.core.supplier.application.dto.SupplierResponse
import com.delique.core.supplier.application.usecase.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/suppliers")
class SupplierController(
    private val createSupplier: CreateSupplierUseCase,
    private val updateSupplier: UpdateSupplierUseCase,
    private val deleteSupplier: DeleteSupplierUseCase,
    private val listSuppliers: ListSuppliersUseCase,
) {
    @GetMapping
    fun list(): List<SupplierResponse> = listSuppliers.execute()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: SupplierRequest): SupplierResponse = createSupplier.execute(request)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: SupplierRequest): SupplierResponse =
        updateSupplier.execute(UpdateSupplierInput(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = deleteSupplier.execute(id)
}
