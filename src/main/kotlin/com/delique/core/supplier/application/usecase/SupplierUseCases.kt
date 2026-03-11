package com.delique.core.supplier.application.usecase

import com.delique.core.supplier.application.dto.SupplierRequest
import com.delique.core.supplier.application.dto.SupplierResponse
import com.delique.core.supplier.application.dto.toResponse
import com.delique.core.supplier.domain.model.Supplier
import com.delique.core.supplier.domain.port.SupplierRepository
import com.delique.core.shared.domain.NoOutputUseCase
import com.delique.core.shared.domain.UseCase
import com.delique.core.shared.infrastructure.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateSupplierUseCase(
    private val supplierRepository: SupplierRepository,
) : UseCase<SupplierRequest, SupplierResponse> {

    @Transactional
    override fun execute(input: SupplierRequest): SupplierResponse {
        val supplier = Supplier(
            name                    = input.name,
            website                 = input.website,
            emails                  = input.emails.toMutableList(),
            phones                  = input.phones.toMutableList(),
            freight                 = input.freight,
            minFreeFreight          = input.minFreeFreight,
            minOrderValue           = input.minOrderValue,
            scraperExcluded         = input.scraperExcluded,
            scraperSuccessSelectors = input.scraperSuccessSelectors,
        )
        return supplierRepository.save(supplier).toResponse()
    }
}

data class UpdateSupplierInput(val id: Long, val request: SupplierRequest)

@Component
class UpdateSupplierUseCase(
    private val supplierRepository: SupplierRepository,
) : UseCase<UpdateSupplierInput, SupplierResponse> {

    @Transactional
    override fun execute(input: UpdateSupplierInput): SupplierResponse {
        val supplier = supplierRepository.findById(input.id)
            ?: throw NotFoundException("Supplier not found: ${input.id}")

        supplier.name                    = input.request.name
        supplier.website                 = input.request.website
        supplier.emails                  = input.request.emails.toMutableList()
        supplier.phones                  = input.request.phones.toMutableList()
        supplier.freight                 = input.request.freight
        supplier.minFreeFreight          = input.request.minFreeFreight
        supplier.minOrderValue           = input.request.minOrderValue
        supplier.scraperExcluded         = input.request.scraperExcluded
        supplier.scraperSuccessSelectors = input.request.scraperSuccessSelectors

        return supplierRepository.save(supplier).toResponse()
    }
}

@Component
class DeleteSupplierUseCase(
    private val supplierRepository: SupplierRepository,
) : NoOutputUseCase<Long> {

    @Transactional
    override fun execute(input: Long) {
        if (!supplierRepository.existsById(input)) throw NotFoundException("Supplier not found: $input")
        supplierRepository.delete(input)
    }
}

@Component
class ListSuppliersUseCase(
    private val supplierRepository: SupplierRepository,
) {
    @Transactional(readOnly = true)
    fun execute(): List<SupplierResponse> = supplierRepository.findAll().map { it.toResponse() }
}
