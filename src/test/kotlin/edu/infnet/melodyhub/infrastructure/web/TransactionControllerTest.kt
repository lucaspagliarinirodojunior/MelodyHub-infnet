package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.transaction.TransactionService
import edu.infnet.melodyhub.application.transaction.dto.CreateTransactionRequest
import edu.infnet.melodyhub.application.transaction.dto.TransactionResponse
import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.TransactionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class TransactionControllerTest {

    private lateinit var transactionService: TransactionService
    private lateinit var transactionController: TransactionController

    @BeforeEach
    fun setup() {
        transactionService = mock()
        transactionController = TransactionController(transactionService)
    }

    private fun createTransactionResponse(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        amount: BigDecimal = BigDecimal("9.90"),
        status: TransactionStatus = TransactionStatus.PENDING
    ): TransactionResponse {
        return TransactionResponse(
            id = id,
            userId = userId,
            amount = amount,
            subscriptionType = SubscriptionType.BASIC,
            status = status,
            fraudReason = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `should create transaction successfully`() {
        val userId = UUID.randomUUID()
        val creditCardId = 1L
        val request = CreateTransactionRequest(
            userId = userId,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = creditCardId
        )
        val response = createTransactionResponse(userId = userId)

        whenever(transactionService.createTransaction(request)).thenReturn(response)

        val result = transactionController.createTransaction(request)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body)
        verify(transactionService).createTransaction(request)
    }

    @Test
    fun `should get transaction by id`() {
        val transactionId = UUID.randomUUID()
        val response = createTransactionResponse(id = transactionId)

        whenever(transactionService.getTransactionById(transactionId)).thenReturn(response)

        val result = transactionController.getTransactionById(transactionId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(transactionService).getTransactionById(transactionId)
    }

    @Test
    fun `should get all transactions`() {
        val transactions = listOf(
            createTransactionResponse(),
            createTransactionResponse()
        )

        whenever(transactionService.getAllTransactions()).thenReturn(transactions)

        val result = transactionController.getAllTransactions()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
        verify(transactionService).getAllTransactions()
    }

    @Test
    fun `should get transactions by user id`() {
        val userId = UUID.randomUUID()
        val transactions = listOf(
            createTransactionResponse(userId = userId),
            createTransactionResponse(userId = userId)
        )

        whenever(transactionService.getTransactionsByUserId(userId)).thenReturn(transactions)

        val result = transactionController.getTransactionsByUserId(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
        verify(transactionService).getTransactionsByUserId(userId)
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        val exception = IllegalArgumentException("Valor da transação inválido")

        val result = transactionController.handleIllegalArgument(exception)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("Valor da transação inválido", result.body?.message)
    }
}
