package edu.infnet.melodyhub.application.creditcard

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardRepository
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import edu.infnet.melodyhub.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class CreditCardService(
    private val creditCardRepository: CreditCardRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(dto: CreateCreditCardDTO): CreditCardResponseDTO {
        // Validar se usuário existe
        userRepository.findById(dto.userId)
            ?: throw IllegalArgumentException("Usuário não encontrado")

        // Validar número do cartão usando algoritmo de Luhn
        if (!CreditCard.isValidCardNumber(dto.cardNumber)) {
            throw IllegalArgumentException("Número de cartão inválido")
        }

        // Validar CVV (3 ou 4 dígitos)
        if (!dto.cvv.matches(Regex("^\\d{3,4}$"))) {
            throw IllegalArgumentException("CVV inválido")
        }

        // Validar mês (1-12)
        if (dto.expirationMonth !in 1..12) {
            throw IllegalArgumentException("Mês de expiração inválido")
        }

        // Validar ano (não pode ser ano passado)
        val currentYear = YearMonth.now().year
        if (dto.expirationYear < currentYear) {
            throw IllegalArgumentException("Ano de expiração inválido")
        }

        // Validar se não está expirado
        val cardExpiration = YearMonth.of(dto.expirationYear, dto.expirationMonth)
        if (cardExpiration.isBefore(YearMonth.now())) {
            throw IllegalArgumentException("Cartão expirado")
        }

        // Validar nome do titular (mínimo 3 caracteres)
        if (dto.cardHolderName.trim().length < 3) {
            throw IllegalArgumentException("Nome do titular inválido")
        }

        // Identificar bandeira
        val brand = CreditCard.identifyBrand(dto.cardNumber)
        if (brand == "UNKNOWN") {
            throw IllegalArgumentException("Bandeira do cartão não reconhecida")
        }

        // Mascara o número do cartão
        val maskedNumber = CreditCard.maskCardNumber(dto.cardNumber)

        val creditCard = CreditCard(
            userId = dto.userId,
            cardNumber = maskedNumber,
            cardHolderName = dto.cardHolderName.uppercase(),
            expirationMonth = dto.expirationMonth,
            expirationYear = dto.expirationYear,
            cvv = dto.cvv, // Em produção, seria hash
            status = CreditCardStatus.ACTIVE,
            brand = brand
        )

        val saved = creditCardRepository.save(creditCard)
        return CreditCardResponseDTO.fromDomain(saved)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): CreditCardResponseDTO {
        val creditCard = creditCardRepository.findById(id)
            ?: throw NoSuchElementException("Cartão não encontrado")
        return CreditCardResponseDTO.fromDomain(creditCard)
    }

    @Transactional(readOnly = true)
    fun findByUserId(userId: java.util.UUID): List<CreditCardResponseDTO> {
        return creditCardRepository.findByUserId(userId)
            .map { CreditCardResponseDTO.fromDomain(it) }
    }

    @Transactional(readOnly = true)
    fun findActiveByUserId(userId: java.util.UUID): List<CreditCardResponseDTO> {
        return creditCardRepository.findActiveByUserId(userId)
            .map { CreditCardResponseDTO.fromDomain(it) }
    }

    @Transactional
    fun updateStatus(id: Long, dto: UpdateCreditCardStatusDTO): CreditCardResponseDTO {
        val creditCard = creditCardRepository.findById(id)
            ?: throw NoSuchElementException("Cartão não encontrado")

        when (dto.status) {
            CreditCardStatus.ACTIVE -> creditCard.activate()
            CreditCardStatus.INACTIVE -> creditCard.deactivate()
            CreditCardStatus.BLOCKED -> creditCard.status = CreditCardStatus.BLOCKED
        }

        val updated = creditCardRepository.save(creditCard)
        return CreditCardResponseDTO.fromDomain(updated)
    }

    @Transactional
    fun delete(id: Long) {
        val creditCard = creditCardRepository.findById(id)
            ?: throw NoSuchElementException("Cartão não encontrado")
        creditCardRepository.delete(creditCard)
    }

    @Transactional(readOnly = true)
    fun hasActiveCard(userId: java.util.UUID): Boolean {
        val activeCards = creditCardRepository.findActiveByUserId(userId)
        return activeCards.any { !it.isExpired() }
    }
}
