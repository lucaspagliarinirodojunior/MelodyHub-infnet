package edu.infnet.melodyhub.application.transaction

import edu.infnet.melodyhub.application.transaction.dto.CreateTransactionRequest
import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionRepository
import edu.infnet.melodyhub.domain.transaction.TransactionStatus
import edu.infnet.melodyhub.infrastructure.events.DomainEventPublisher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.util.*

class TransactionServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var antiFraudService: AntiFraudService
    private lateinit var eventPublisher: DomainEventPublisher
    private lateinit var transactionService: TransactionService

    @BeforeEach
    fun setup() {
        transactionRepository = mock()
        antiFraudService = mock()
        eventPublisher = mock()
        transactionService = TransactionService(
            transactionRepository,
            antiFraudService,
            eventPublisher
        )
    }

    @Test
    fun `should create transaction successfully when fraud check passes`() {
        val userId = UUID.randomUUID()
        val creditCardId = 1L
        val request = CreateTransactionRequest(
            userId = userId,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = creditCardId
        )

        val transaction = Transaction(
            id = UUID.randomUUID(),
            userId = userId,
            amount = SubscriptionType.BASIC.monthlyPrice,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = creditCardId
        )

        whenever(antiFraudService.validateTransaction(any())).thenReturn(
            FraudCheckResult(isValid = true, reason = null)
        )
        whenever(transactionRepository.save(any())).thenReturn(transaction)

        val response = transactionService.createTransaction(request)

        assertNotNull(response)
        assertEquals(userId, response.userId)
        assertEquals(SubscriptionType.BASIC, response.subscriptionType)
        verify(transactionRepository).save(any())
        verify(eventPublisher, atLeastOnce()).publish(any())
    }

    @Test
    fun `should reject transaction when fraud check fails`() {
        val userId = UUID.randomUUID()
        val creditCardId = 1L
        val request = CreateTransactionRequest(
            userId = userId,
            subscriptionType = SubscriptionType.PREMIUM,
            creditCardId = creditCardId
        )

        whenever(antiFraudService.validateTransaction(any())).thenReturn(
            FraudCheckResult(isValid = false, reason = "Alta frequência detectada")
        )

        // Capturar o objeto salvo para verificar seu estado
        whenever(transactionRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument(0) as Transaction
        }

        val response = transactionService.createTransaction(request)

        assertNotNull(response)
        assertEquals(TransactionStatus.REJECTED, response.status)
        assertNotNull(response.fraudReason)
        assertTrue(response.fraudReason!!.contains("Alta frequência") || response.fraudReason!!.contains("detectada"))
        verify(transactionRepository).save(any())
    }

    @Test
    fun `should get transaction by id`() {
        val transactionId = UUID.randomUUID()
        val transaction = Transaction(
            id = transactionId,
            userId = UUID.randomUUID(),
            amount = SubscriptionType.BASIC.monthlyPrice,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        whenever(transactionRepository.findById(transactionId)).thenReturn(transaction)

        val response = transactionService.getTransactionById(transactionId)

        assertEquals(transactionId, response.id)
        verify(transactionRepository).findById(transactionId)
    }

    @Test
    fun `should throw exception when transaction not found`() {
        val transactionId = UUID.randomUUID()

        whenever(transactionRepository.findById(transactionId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            transactionService.getTransactionById(transactionId)
        }

        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `should get all transactions`() {
        val transactions = listOf(
            Transaction(UUID.randomUUID(), UUID.randomUUID(), BigDecimal("9.90"), SubscriptionType.BASIC, 1L),
            Transaction(UUID.randomUUID(), UUID.randomUUID(), BigDecimal("19.90"), SubscriptionType.PREMIUM, 2L)
        )

        whenever(transactionRepository.findAll()).thenReturn(transactions)

        val response = transactionService.getAllTransactions()

        assertEquals(2, response.size)
        verify(transactionRepository).findAll()
    }

    @Test
    fun `should get transactions by user id`() {
        val userId = UUID.randomUUID()
        val transactions = listOf(
            Transaction(UUID.randomUUID(), userId, BigDecimal("9.90"), SubscriptionType.BASIC, 1L)
        )

        whenever(transactionRepository.findByUserId(userId)).thenReturn(transactions)

        val response = transactionService.getTransactionsByUserId(userId)

        assertEquals(1, response.size)
        assertEquals(userId, response[0].userId)
        verify(transactionRepository).findByUserId(userId)
    }
}
