package edu.infnet.melodyhub.infrastructure.transaction

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

class TransactionRepositoryImplTest {

    private lateinit var jpaRepository: JpaTransactionRepository
    private lateinit var transactionRepository: TransactionRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaRepository = mock()
        transactionRepository = TransactionRepositoryImpl(jpaRepository)
    }

    @Test
    fun `should save transaction`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            amount = SubscriptionType.BASIC.monthlyPrice,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L,
            status = TransactionStatus.PENDING
        )

        whenever(jpaRepository.save(transaction)).thenReturn(transaction)

        val result = transactionRepository.save(transaction)

        assertEquals(transaction, result)
        verify(jpaRepository).save(transaction)
    }

    @Test
    fun `should find transaction by id`() {
        val transactionId = UUID.randomUUID()
        val transaction = Transaction(
            id = transactionId,
            userId = UUID.randomUUID(),
            amount = SubscriptionType.BASIC.monthlyPrice,
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        whenever(jpaRepository.findById(transactionId)).thenReturn(Optional.of(transaction))

        val result = transactionRepository.findById(transactionId)

        assertEquals(transaction, result)
        verify(jpaRepository).findById(transactionId)
    }

    @Test
    fun `should return null when transaction not found by id`() {
        val transactionId = UUID.randomUUID()

        whenever(jpaRepository.findById(transactionId)).thenReturn(Optional.empty())

        val result = transactionRepository.findById(transactionId)

        assertNull(result)
        verify(jpaRepository).findById(transactionId)
    }

    @Test
    fun `should find all transactions`() {
        val transactions = listOf(
            Transaction(UUID.randomUUID(), UUID.randomUUID(), SubscriptionType.BASIC.monthlyPrice, SubscriptionType.BASIC, 1L),
            Transaction(UUID.randomUUID(), UUID.randomUUID(), SubscriptionType.PREMIUM.monthlyPrice, SubscriptionType.PREMIUM, 2L)
        )

        whenever(jpaRepository.findAll()).thenReturn(transactions)

        val result = transactionRepository.findAll()

        assertEquals(2, result.size)
        assertEquals(transactions, result)
        verify(jpaRepository).findAll()
    }

    @Test
    fun `should find transactions by user id`() {
        val userId = UUID.randomUUID()
        val transactions = listOf(
            Transaction(UUID.randomUUID(), userId, SubscriptionType.BASIC.monthlyPrice, SubscriptionType.BASIC, 1L)
        )

        whenever(jpaRepository.findByUserId(userId)).thenReturn(transactions)

        val result = transactionRepository.findByUserId(userId)

        assertEquals(1, result.size)
        assertEquals(transactions, result)
        verify(jpaRepository).findByUserId(userId)
    }

    @Test
    fun `should find approved transactions by user id`() {
        val userId = UUID.randomUUID()
        val transactions = listOf(
            Transaction(UUID.randomUUID(), userId, SubscriptionType.BASIC.monthlyPrice, SubscriptionType.BASIC, 1L)
        )

        whenever(jpaRepository.findApprovedByUserId(userId)).thenReturn(transactions)

        val result = transactionRepository.findApprovedByUserId(userId)

        assertEquals(1, result.size)
        assertEquals(transactions, result)
        verify(jpaRepository).findApprovedByUserId(userId)
    }

    @Test
    fun `should count transactions by user id and created at after`() {
        val userId = UUID.randomUUID()
        val since = LocalDateTime.now().minusHours(1)

        whenever(jpaRepository.countByUserIdAndCreatedAtAfter(userId, since)).thenReturn(3L)

        val result = transactionRepository.countByUserIdAndCreatedAtAfter(userId, since)

        assertEquals(3L, result)
        verify(jpaRepository).countByUserIdAndCreatedAtAfter(userId, since)
    }

    @Test
    fun `should count transactions by multiple criteria`() {
        val userId = UUID.randomUUID()
        val amount = SubscriptionType.BASIC.monthlyPrice
        val subscriptionType = SubscriptionType.BASIC
        val since = LocalDateTime.now().minusMinutes(1)

        whenever(
            jpaRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
                userId,
                amount,
                subscriptionType,
                since
            )
        ).thenReturn(2L)

        val result = transactionRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            userId,
            amount,
            subscriptionType,
            since
        )

        assertEquals(2L, result)
        verify(jpaRepository).countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            userId,
            amount,
            subscriptionType,
            since
        )
    }
}
