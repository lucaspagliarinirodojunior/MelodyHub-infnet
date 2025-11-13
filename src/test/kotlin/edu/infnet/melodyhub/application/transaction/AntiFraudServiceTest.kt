package edu.infnet.melodyhub.application.transaction

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardRepository
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionRepository
import edu.infnet.melodyhub.domain.transaction.TransactionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AntiFraudServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var antiFraudService: AntiFraudService

    @BeforeEach
    fun setup() {
        transactionRepository = mock()
        creditCardRepository = mock()
        antiFraudService = AntiFraudService(transactionRepository, creditCardRepository)
    }

    private fun createValidTransaction(
        userId: UUID = UUID.randomUUID(),
        amount: BigDecimal = BigDecimal("9.90"),
        creditCardId: Long = 1L
    ): Transaction {
        return Transaction(
            userId = userId,
            amount = amount,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = creditCardId
        )
    }

    private fun createValidCreditCard(
        userId: UUID,
        cardId: Long = 1L
    ): CreditCard {
        val futureDate = YearMonth.now().plusYears(1)
        return CreditCard(
            id = cardId,
            userId = userId,
            cardNumber = "**** **** **** 1234",
            cardHolderName = "Test User",
            expirationMonth = futureDate.monthValue,
            expirationYear = futureDate.year,
            cvv = "123",
            brand = "VISA",
            status = CreditCardStatus.ACTIVE
        )
    }

    @Test
    fun `should pass validation for valid transaction`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)
        val creditCard = createValidCreditCard(userId)

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)
        whenever(creditCardRepository.findById(1L)).thenReturn(creditCard)
        whenever(transactionRepository.findApprovedByUserId(userId)).thenReturn(emptyList())

        val result = antiFraudService.validateTransaction(transaction)

        assertTrue(result.isValid)
        assertNull(result.reason)
    }

    @Test
    fun `should reject transaction with zero amount`() {
        val transaction = createValidTransaction(amount = BigDecimal.ZERO)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Valor da transação deve ser positivo", result.reason)
    }

    @Test
    fun `should reject transaction with negative amount`() {
        val transaction = createValidTransaction(amount = BigDecimal("-10.00"))

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Valor da transação deve ser positivo", result.reason)
    }

    @Test
    fun `should reject transaction exceeding maximum amount`() {
        val transaction = createValidTransaction(amount = BigDecimal("150.00"))

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Valor da transação excede o limite permitido de R$ 100,00", result.reason)
    }

    @Test
    fun `should reject transaction with high frequency in 2 minutes`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(3)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Alta frequência detectada: mais de 3 transações em 2 minutos", result.reason)
    }

    @Test
    fun `should reject duplicate transaction in 2 minutes`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(2)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Transação duplicada detectada: mesma assinatura tentada 2 vezes em 2 minutos", result.reason)
    }

    @Test
    fun `should reject transaction when daily limit exceeded`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(0)  // First call (2 minutes check)
            .thenReturn(5)  // Second call (daily check)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Limite diário de transações excedido (máximo 5 por dia)", result.reason)
    }

    @Test
    fun `should reject transaction when credit card not found`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId, creditCardId = 999L)

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)
        whenever(creditCardRepository.findById(999L)).thenReturn(null)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Cartão de crédito não encontrado", result.reason)
    }

    @Test
    fun `should reject transaction when credit card is inactive`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)
        val creditCard = createValidCreditCard(userId).apply {
            status = CreditCardStatus.INACTIVE
        }

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)
        whenever(creditCardRepository.findById(1L)).thenReturn(creditCard)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Cartão de crédito não está ativo", result.reason)
    }

    @Test
    fun `should reject transaction when credit card is expired`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)
        val pastDate = YearMonth.now().minusMonths(1)
        val creditCard = CreditCard(
            id = 1L,
            userId = userId,
            cardNumber = "**** **** **** 1234",
            cardHolderName = "Test User",
            expirationMonth = pastDate.monthValue,
            expirationYear = pastDate.year,
            cvv = "123",
            brand = "VISA",
            status = CreditCardStatus.ACTIVE
        )

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)
        whenever(creditCardRepository.findById(1L)).thenReturn(creditCard)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Cartão de crédito expirado", result.reason)
    }

    @Test
    fun `should reject transaction when card does not belong to user`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)
        val creditCard = createValidCreditCard(userId = otherUserId)

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)
        whenever(creditCardRepository.findById(1L)).thenReturn(creditCard)

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Cartão de crédito não pertence ao usuário", result.reason)
    }

    @Test
    fun `should reject transaction when user already has active plan`() {
        val userId = UUID.randomUUID()
        val transaction = createValidTransaction(userId = userId)
        val creditCard = createValidCreditCard(userId)
        val approvedTransaction = Transaction(
            userId = userId,
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L,
            status = TransactionStatus.APPROVED
        )

        whenever(transactionRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0)
        whenever(transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            eq(userId), any(), any(), any()
        )).thenReturn(0)
        whenever(creditCardRepository.findById(1L)).thenReturn(creditCard)
        whenever(transactionRepository.findApprovedByUserId(userId)).thenReturn(listOf(approvedTransaction))

        val result = antiFraudService.validateTransaction(transaction)

        assertFalse(result.isValid)
        assertEquals("Usuário já possui um plano ativo. Apenas um plano por vez é permitido.", result.reason)
    }
}
