package edu.infnet.melodyhub.application.creditcard

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import java.util.UUID

data class CreateCreditCardDTO(
    val userId: UUID,
    val cardNumber: String,
    val cardHolderName: String,
    val expirationMonth: Int,
    val expirationYear: Int,
    val cvv: String
)

data class CreditCardResponseDTO(
    val id: Long,
    val userId: UUID,
    val maskedCardNumber: String,
    val cardHolderName: String,
    val expirationMonth: Int,
    val expirationYear: Int,
    val status: CreditCardStatus,
    val brand: String,
    val isExpired: Boolean,
    val isValid: Boolean
) {
    companion object {
        fun fromDomain(creditCard: CreditCard): CreditCardResponseDTO {
            return CreditCardResponseDTO(
                id = creditCard.id!!,
                userId = creditCard.userId,
                maskedCardNumber = creditCard.cardNumber,
                cardHolderName = creditCard.cardHolderName,
                expirationMonth = creditCard.expirationMonth,
                expirationYear = creditCard.expirationYear,
                status = creditCard.status,
                brand = creditCard.brand,
                isExpired = creditCard.isExpired(),
                isValid = creditCard.isValid()
            )
        }
    }
}

data class UpdateCreditCardStatusDTO(
    val status: CreditCardStatus
)
