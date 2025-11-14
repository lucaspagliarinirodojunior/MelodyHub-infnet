package edu.infnet.melodyhub.application.transaction

import edu.infnet.melodyhub.application.transaction.dto.CreateTransactionRequest
import edu.infnet.melodyhub.application.transaction.dto.TransactionResponse
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionRepository
import edu.infnet.melodyhub.infrastructure.observability.UserContextEnricher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Application Layer - Transaction Use Cases
 *
 * Service responsible for transaction processing business logic.
 * Logs transaction operations for audit and fraud detection analysis.
 */
@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val antiFraudService: AntiFraudService,
    private val eventPublisher: edu.infnet.melodyhub.infrastructure.events.DomainEventPublisher,
    private val userContextEnricher: UserContextEnricher
) {
    private val logger = LoggerFactory.getLogger(TransactionService::class.java)

    @Transactional
    fun createTransaction(request: CreateTransactionRequest): TransactionResponse {
        logger.info(
            "Creating transaction: userId={}, subscriptionType={}, amount={}",
            request.userId,
            request.subscriptionType,
            request.subscriptionType.monthlyPrice
        )

        // Enriquecer contexto de logging
        userContextEnricher.enrichWithUserContext(request.userId.toString(), null, null)

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
            logger.warn(
                "Transaction rejected by anti-fraud: userId={}, reason={}",
                request.userId,
                fraudCheckResult.reason
            )
            // Aggregate publica seu próprio evento de fraude
            transaction.reject(fraudCheckResult.reason ?: "Validação de antifraude falhou")
        } else {
            // Calcular novo role baseado no tipo de assinatura
            val newRole = calculateNewRole(request.subscriptionType)

            logger.info(
                "Transaction approved: userId={}, subscriptionType={}, newRole={}",
                request.userId,
                request.subscriptionType,
                newRole
            )

            // Aggregate publica seu próprio evento de aprovação
            // O evento será consumido pelo Account Context que atualizará o User
            transaction.approve(newRole)
        }
        val eventsToPublish = transaction.getEvents().toList()

        // Salvar transação
        val savedTransaction = transactionRepository.save(transaction)

        // Adicionar contexto de transação aos logs
        savedTransaction.id?.let { transactionId ->
            userContextEnricher.enrichWithTransactionContext(transactionId.toString())
            logger.info("Transaction saved: transactionId={}", transactionId)
        }

        // Registrar evento de validação (após ter ID salvo)
        savedTransaction.recordValidation(fraudCheckResult.isValid, fraudCheckResult.reason)

        // Coletar evento de validação
        val validationEvents = savedTransaction.getAndClearEvents()

        // Publicar TODOS os eventos (approve/reject + validated)
        (eventsToPublish + validationEvents).forEach { event ->
            eventPublisher.publish(event)
        }

        return TransactionResponse.from(savedTransaction)
    }

    /**
     * Calcula novo role baseado no tipo de assinatura.
     * Lógica de domínio encapsulada no service.
     */
    private fun calculateNewRole(subscriptionType: edu.infnet.melodyhub.domain.transaction.SubscriptionType): edu.infnet.melodyhub.domain.user.UserRole {
        return when (subscriptionType) {
            edu.infnet.melodyhub.domain.transaction.SubscriptionType.BASIC ->
                edu.infnet.melodyhub.domain.user.UserRole.BASIC
            edu.infnet.melodyhub.domain.transaction.SubscriptionType.PREMIUM ->
                edu.infnet.melodyhub.domain.user.UserRole.PREMIUM
        }
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

/**
 * Application Layer - Anti-Fraud Use Cases
 *
 * Service responsible for fraud detection business rules.
 * Logs fraud detection events for security analysis and pattern detection.
 */
@Service
class AntiFraudService(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: edu.infnet.melodyhub.domain.creditcard.CreditCardRepository
) {
    private val logger = LoggerFactory.getLogger(AntiFraudService::class.java)

    fun validateTransaction(transaction: Transaction): FraudCheckResult {
        logger.debug("Starting anti-fraud validation for userId={}", transaction.userId)

        // Regra 1: Validar se o valor é positivo
        if (transaction.amount <= BigDecimal.ZERO) {
            logger.warn(
                "Anti-fraud rule violated: negative amount - userId={}, amount={}",
                transaction.userId,
                transaction.amount
            )
            return FraudCheckResult(
                isValid = false,
                reason = "Valor da transação deve ser positivo"
            )
        }

        // Regra 2: Validar limite máximo (R$ 100,00 para demonstração)
        if (transaction.amount > BigDecimal("100.00")) {
            logger.warn(
                "Anti-fraud rule violated: amount exceeds limit - userId={}, amount={}",
                transaction.userId,
                transaction.amount
            )
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
            logger.warn(
                "Anti-fraud rule violated: high frequency - userId={}, count={} in 2 minutes",
                transaction.userId,
                recentTransactionsCount
            )
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
        if (creditCard.userId != transaction.userId) {
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
        logger.info(
            "Anti-fraud validation passed for userId={}, amount={}",
            transaction.userId,
            transaction.amount
        )
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
