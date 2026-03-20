package com.delique.core.pricing.application

import com.delique.core.pricing.application.dto.InstallmentFeeDto
import com.delique.core.pricing.application.dto.PaymentMethodConfigUpdateDto
import com.delique.core.pricing.application.dto.PaymentMethodDto
import com.delique.core.pricing.domain.model.CreditCardInstallmentFee
import com.delique.core.pricing.domain.model.PaymentMethodConfig
import com.delique.core.pricing.infrastructure.persistence.CreditCardInstallmentFeeJpa
import com.delique.core.pricing.infrastructure.persistence.PaymentMethodConfigJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val FIXED_METHODS = listOf(
    "MONEY" to "Dinheiro",
    "PIX" to "PIX",
    "CREDIT_CARD" to "Cartão de Crédito",
    "DEBIT_CARD" to "Cartão de Débito",
)

@Service
class PaymentMethodApplicationService(
    private val paymentMethodConfigJpa: PaymentMethodConfigJpa,
    private val creditCardInstallmentFeeJpa: CreditCardInstallmentFeeJpa,
) {
    fun getAllPaymentMethods(): List<PaymentMethodDto> {
        val configs = paymentMethodConfigJpa.findAll().associateBy { it.paymentMethod }
        val installmentFees = creditCardInstallmentFeeJpa.findAllByOrderByInstallmentsAsc()
            .associateBy { it.installments }

        return FIXED_METHODS.map { (code, name) ->
            val config = configs[code]
            val installmentFeesList = if (code == "CREDIT_CARD") {
                (1..12).map { i ->
                    InstallmentFeeDto(
                        installments = i,
                        feePercentage = installmentFees[i]?.feePercentage ?: BigDecimal.ZERO,
                    )
                }
            } else {
                emptyList()
            }
            PaymentMethodDto(
                id = config?.id ?: 0L,
                code = code,
                name = name,
                discountPercentage = config?.discountPercentage,
                feePercentage = config?.feePercentage,
                installmentFees = installmentFeesList,
            )
        }
    }

    @Transactional
    fun updateConfig(update: PaymentMethodConfigUpdateDto) {
        var config = paymentMethodConfigJpa.findByPaymentMethod(update.paymentMethod)
        if (config == null) {
            config = paymentMethodConfigJpa.save(PaymentMethodConfig(paymentMethod = update.paymentMethod))
        }
        config.discountPercentage = update.discountPercentage
        config.feePercentage = update.feePercentage
        paymentMethodConfigJpa.save(config)

        if (update.paymentMethod == "CREDIT_CARD" && update.installmentFees.isNotEmpty()) {
            update.installmentFees.forEach { dto ->
                var fee = creditCardInstallmentFeeJpa.findByInstallments(dto.installments)
                if (fee == null) {
                    fee = CreditCardInstallmentFee(installments = dto.installments, feePercentage = dto.feePercentage)
                } else {
                    fee.feePercentage = dto.feePercentage
                }
                creditCardInstallmentFeeJpa.save(fee)
            }
        }
    }

    @Transactional
    fun updateConfigBulk(updates: List<PaymentMethodConfigUpdateDto>) {
        updates.forEach { updateConfig(it) }
    }
}
