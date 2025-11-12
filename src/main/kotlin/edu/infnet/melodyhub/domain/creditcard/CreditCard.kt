package edu.infnet.melodyhub.domain.creditcard

import jakarta.persistence.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Entity
@Table(name = "credit_cards")
data class CreditCard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val cardNumber: String, // Armazenado com máscara (últimos 4 dígitos)

    @Column(nullable = false)
    val cardHolderName: String,

    @Column(nullable = false)
    val expirationMonth: Int,

    @Column(nullable = false)
    val expirationYear: Int,

    @Column(nullable = false)
    val cvv: String, // Normalmente seria hash, mas é simulação

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CreditCardStatus = CreditCardStatus.ACTIVE,

    @Column(nullable = false)
    val brand: String // VISA, MASTERCARD, etc
) {
    companion object {
        /**
         * Implementação do algoritmo de Luhn para validação de número de cartão
         * https://en.wikipedia.org/wiki/Luhn_algorithm
         */
        fun isValidCardNumber(cardNumber: String): Boolean {
            val digits = cardNumber.replace(" ", "").replace("-", "")

            if (digits.length < 13 || digits.length > 19) {
                return false
            }

            if (!digits.all { it.isDigit() }) {
                return false
            }

            var sum = 0
            var alternate = false

            for (i in digits.length - 1 downTo 0) {
                var digit = digits[i].toString().toInt()

                if (alternate) {
                    digit *= 2
                    if (digit > 9) {
                        digit -= 9
                    }
                }

                sum += digit
                alternate = !alternate
            }

            return sum % 10 == 0
        }

        /**
         * Identifica a bandeira do cartão baseado no número
         */
        fun identifyBrand(cardNumber: String): String {
            val digits = cardNumber.replace(" ", "").replace("-", "")

            return when {
                digits.startsWith("4") -> "VISA"
                digits.startsWith("5") && digits[1] in '1'..'5' -> "MASTERCARD"
                digits.startsWith("34") || digits.startsWith("37") -> "AMEX"
                digits.startsWith("6011") || digits.startsWith("65") -> "DISCOVER"
                digits.startsWith("35") -> "JCB"
                else -> "UNKNOWN"
            }
        }

        /**
         * Mascara o número do cartão mantendo apenas os últimos 4 dígitos
         */
        fun maskCardNumber(cardNumber: String): String {
            val digits = cardNumber.replace(" ", "").replace("-", "")
            if (digits.length < 4) return "****"
            return "**** **** **** ${digits.takeLast(4)}"
        }
    }

    fun isExpired(): Boolean {
        val currentDate = YearMonth.now()
        val cardExpiration = YearMonth.of(expirationYear, expirationMonth)
        return cardExpiration.isBefore(currentDate)
    }

    fun isActive(): Boolean = status == CreditCardStatus.ACTIVE

    fun activate() {
        status = CreditCardStatus.ACTIVE
    }

    fun deactivate() {
        status = CreditCardStatus.INACTIVE
    }

    fun isValid(): Boolean {
        return isActive() && !isExpired()
    }
}

enum class CreditCardStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED
}
