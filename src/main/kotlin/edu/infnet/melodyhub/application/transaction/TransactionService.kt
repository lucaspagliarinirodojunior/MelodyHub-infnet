package edu.infnet.melodyhub.application.transaction

import edu.infnet.melodyhub.application.transaction.dto.CreateTransactionRequest
import edu.infnet.melodyhub.application.transaction.dto.TransactionResponse
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionRepository
import edu.infnet.melodyhub.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val antiFraudService: AntiFraudService,
    private val eventPublisher: edu.infnet.melodyhub.infrastructure.events.DomainEventPublisher
) {

    @Transactional
    fun createTransaction(request: CreateTransactionRequest): TransactionResponse {
        // Validar se usuário existe
        val user = userRepository.findById(request.userId)
            ?: throw IllegalArgumentException("User not found with ID: ${request.userId}")

        // Obter valor da assinatura
        val amount = request.subscriptionType.monthlyPrice

        // Criar transação
        val transaction = Transaction(
            userId = request.userId,
            amount = amount,
            subscriptionType = request.subscriptionType,
            creditCardId = request.creditCardId
        )

        // Validar regras de antifraude
        val fraudCheckResult = antiFraudService.validateTransaction(transaction)

        if (!fraudCheckResult.isValid) {
            transaction.reject(fraudCheckResult.reason ?: "Validação de antifraude falhou")

            // Publicar evento de fraude detectada
            publishFraudDetectedEvent(transaction, fraudCheckResult.reason)
        } else {
            transaction.approve()

            // Atualizar role do usuário baseado no plano
            val newRole = updateUserRoleBasedOnSubscription(user, request.subscriptionType)

            // Publicar evento de transação aprovada
            publishTransactionApprovedEvent(transaction, newRole)
        }

        // Salvar transação
        val savedTransaction = transactionRepository.save(transaction)

        // Publicar evento de transação validada
        publishTransactionValidatedEvent(savedTransaction, fraudCheckResult)

        return TransactionResponse.from(savedTransaction)
    }

    private fun publishTransactionValidatedEvent(
        transaction: Transaction,
        fraudCheckResult: FraudCheckResult
    ) {
        val event = edu.infnet.melodyhub.domain.events.TransactionValidatedEvent(
            transactionId = transaction.id,
            userId = transaction.userId,
            amount = transaction.amount,
            subscriptionType = transaction.subscriptionType,
            isValid = fraudCheckResult.isValid,
            fraudReason = fraudCheckResult.reason
        )
        eventPublisher.publish(event)
    }

    private fun publishFraudDetectedEvent(transaction: Transaction, fraudReason: String?) {
        val event = edu.infnet.melodyhub.domain.events.FraudDetectedEvent(
            transactionId = transaction.id,
            userId = transaction.userId,
            fraudReason = fraudReason ?: "Motivo desconhecido",
            violatedRules = listOf(fraudReason ?: "Regra desconhecida")
        )
        eventPublisher.publish(event)
    }

    private fun publishTransactionApprovedEvent(transaction: Transaction, newRole: edu.infnet.melodyhub.domain.user.UserRole) {
        val event = edu.infnet.melodyhub.domain.events.TransactionApprovedEvent(
            transactionId = transaction.id,
            userId = transaction.userId,
            subscriptionType = transaction.subscriptionType,
            newUserRole = newRole
        )
        eventPublisher.publish(event)
    }

    private fun updateUserRoleBasedOnSubscription(
        user: edu.infnet.melodyhub.domain.user.User,
        subscriptionType: edu.infnet.melodyhub.domain.transaction.SubscriptionType
    ): edu.infnet.melodyhub.domain.user.UserRole {
        val newRole = when (subscriptionType) {
            edu.infnet.melodyhub.domain.transaction.SubscriptionType.BASIC ->
                edu.infnet.melodyhub.domain.user.UserRole.BASIC
            edu.infnet.melodyhub.domain.transaction.SubscriptionType.PREMIUM ->
                edu.infnet.melodyhub.domain.user.UserRole.PREMIUM
        }

        user.updateRole(newRole)
        userRepository.save(user)

        return newRole
    }

    fun getTransactionById(id: UUID): TransactionResponse {
        val transaction = transactionRepository.findById(id)
            ?: throw IllegalArgumentException("Transaction not found with ID: $id")
        return TransactionResponse.from(transaction)
    }

    fun getAllTransactions(): List<TransactionResponse> {
        return transactionRepository.findAll()
            .map { TransactionResponse.from(it) }
    }

    fun getTransactionsByUserId(userId: UUID): List<TransactionResponse> {
        return transactionRepository.findByUserId(userId)
            .map { TransactionResponse.from(it) }
    }
}

@Service
class AntiFraudService(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: edu.infnet.melodyhub.domain.creditcard.CreditCardRepository,
    private val userRepository: UserRepository
) {

    fun validateTransaction(transaction: Transaction): FraudCheckResult {
        // Regra 1: Validar se o valor é positivo
        if (transaction.amount <= BigDecimal.ZERO) {
            return FraudCheckResult(
                isValid = false,
                reason = "Valor da transação deve ser positivo"
            )
        }

        // Regra 2: Validar limite máximo (R$ 100,00 para demonstração)
        if (transaction.amount > BigDecimal("100.00")) {
            return FraudCheckResult(
                isValid = false,
                reason = "Valor da transação excede o limite permitido de R$ 100,00"
            )
        }

        // Regra 3: Validar múltiplas transações em curto período (mais de 3 em 2 minutos)
        val twoMinutesAgo = LocalDateTime.now().minusMinutes(2)
        val recentTransactionsCount = transactionRepository
            .countByUserIdAndCreatedAtAfter(transaction.userId, twoMinutesAgo)

        if (recentTransactionsCount >= 3) {
            return FraudCheckResult(
                isValid = false,
                reason = "Alta frequência detectada: mais de 3 transações em 2 minutos"
            )
        }

        // Regra 4: Validar transações duplicadas (mesmo valor e tipo em 2 minutos)
        val duplicateTransactionsCount = transactionRepository
            .countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
                transaction.userId,
                transaction.amount,
                transaction.subscriptionType,
                twoMinutesAgo
            )

        if (duplicateTransactionsCount >= 2) {
            return FraudCheckResult(
                isValid = false,
                reason = "Transação duplicada detectada: mesma assinatura tentada 2 vezes em 2 minutos"
            )
        }

        // Regra 5: Validar múltiplas transações no mesmo dia (mais de 5)
        val todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        val todayTransactionsCount = transactionRepository
            .countByUserIdAndCreatedAtAfter(transaction.userId, todayStart)

        if (todayTransactionsCount >= 5) {
            return FraudCheckResult(
                isValid = false,
                reason = "Limite diário de transações excedido (máximo 5 por dia)"
            )
        }

        // Regra 6: Validar se o cartão de crédito existe
        val creditCard = creditCardRepository.findById(transaction.creditCardId)
            ?: return FraudCheckResult(
                isValid = false,
                reason = "Cartão de crédito não encontrado"
            )

        // Regra 7: Validar se o cartão está ativo
        if (!creditCard.isActive()) {
            return FraudCheckResult(
                isValid = false,
                reason = "Cartão de crédito não está ativo"
            )
        }

        // Regra 8: Validar se o cartão está expirado
        if (creditCard.isExpired()) {
            return FraudCheckResult(
                isValid = false,
                reason = "Cartão de crédito expirado"
            )
        }

        // Regra 9: Validar se o cartão pertence ao usuário
        val user = userRepository.findById(transaction.userId)
            ?: return FraudCheckResult(
                isValid = false,
                reason = "Usuário não encontrado"
            )

        if (creditCard.userId != user.id) {
            return FraudCheckResult(
                isValid = false,
                reason = "Cartão de crédito não pertence ao usuário"
            )
        }

        // Regra 10: Validar se o usuário já tem um plano ativo
        val approvedTransactions = transactionRepository.findApprovedByUserId(transaction.userId)
        if (approvedTransactions.isNotEmpty()) {
            return FraudCheckResult(
                isValid = false,
                reason = "Usuário já possui um plano ativo. Apenas um plano por vez é permitido."
            )
        }

        // Todas as regras passaram
        return FraudCheckResult(
            isValid = true,
            reason = null
        )
    }
}

data class FraudCheckResult(
    val isValid: Boolean,
    val reason: String?
)
