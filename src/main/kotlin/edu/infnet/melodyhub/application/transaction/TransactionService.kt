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
    private val antiFraudService: AntiFraudService
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
            subscriptionType = request.subscriptionType
        )

        // Validar regras de antifraude
        val fraudCheckResult = antiFraudService.validateTransaction(transaction)

        if (!fraudCheckResult.isValid) {
            transaction.reject(fraudCheckResult.reason)
        } else {
            transaction.approve()
        }

        // Salvar transação
        val savedTransaction = transactionRepository.save(transaction)

        return TransactionResponse.from(savedTransaction)
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
    private val transactionRepository: TransactionRepository
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

        // Regra 3: Validar múltiplas transações em curto período (mais de 3 em 1 minuto)
        val oneMinuteAgo = LocalDateTime.now().minusMinutes(1)
        val recentTransactionsCount = transactionRepository
            .countByUserIdAndCreatedAtAfter(transaction.userId, oneMinuteAgo)

        if (recentTransactionsCount >= 3) {
            return FraudCheckResult(
                isValid = false,
                reason = "Múltiplas transações detectadas em curto período (mais de 3 em 1 minuto)"
            )
        }

        // Regra 4: Validar múltiplas transações no mesmo dia (mais de 5)
        val todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        val todayTransactionsCount = transactionRepository
            .countByUserIdAndCreatedAtAfter(transaction.userId, todayStart)

        if (todayTransactionsCount >= 5) {
            return FraudCheckResult(
                isValid = false,
                reason = "Limite diário de transações excedido (máximo 5 por dia)"
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
