package edu.infnet.melodyhub.infrastructure.creditcard

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.*

class CreditCardRepositoryImplTest {

    private lateinit var jpaRepository: JpaCreditCardRepository
    private lateinit var creditCardRepository: CreditCardRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaRepository = mock()
        creditCardRepository = CreditCardRepositoryImpl(jpaRepository)
    }

    @Test
    fun `should save credit card`() {
        val creditCard = CreditCard(
            id = 1L,
            userId = UUID.randomUUID(),
            cardNumber = "4111111111111111",
            cardHolderName = "Test User",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            status = CreditCardStatus.ACTIVE,
            brand = "VISA"
        )

        whenever(jpaRepository.save(creditCard)).thenReturn(creditCard)

        val result = creditCardRepository.save(creditCard)

        assertEquals(creditCard, result)
        verify(jpaRepository).save(creditCard)
    }

    @Test
    fun `should find credit card by id`() {
        val cardId = 1L
        val creditCard = CreditCard(
            id = cardId,
            userId = UUID.randomUUID(),
            cardNumber = "4111111111111111",
            cardHolderName = "Test User",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            status = CreditCardStatus.ACTIVE,
            brand = "VISA"
        )

        whenever(jpaRepository.findById(cardId)).thenReturn(Optional.of(creditCard))

        val result = creditCardRepository.findById(cardId)

        assertEquals(creditCard, result)
        verify(jpaRepository).findById(cardId)
    }

    @Test
    fun `should return null when credit card not found by id`() {
        val cardId = 999L

        whenever(jpaRepository.findById(cardId)).thenReturn(Optional.empty())

        val result = creditCardRepository.findById(cardId)

        assertNull(result)
        verify(jpaRepository).findById(cardId)
    }

    @Test
    fun `should find credit cards by user id`() {
        val userId = UUID.randomUUID()
        val cards = listOf(
            CreditCard(1L, userId, "4111111111111111", "User", 12, 2025, "123", CreditCardStatus.ACTIVE, "VISA"),
            CreditCard(2L, userId, "5500000000000004", "User", 12, 2025, "456", CreditCardStatus.BLOCKED, "MASTERCARD")
        )

        whenever(jpaRepository.findByUserId(userId)).thenReturn(cards)

        val result = creditCardRepository.findByUserId(userId)

        assertEquals(2, result.size)
        assertEquals(cards, result)
        verify(jpaRepository).findByUserId(userId)
    }

    @Test
    fun `should find active credit cards by user id`() {
        val userId = UUID.randomUUID()
        val activeCards = listOf(
            CreditCard(1L, userId, "4111111111111111", "User", 12, 2025, "123", CreditCardStatus.ACTIVE, "VISA")
        )

        whenever(jpaRepository.findActiveByUserId(userId)).thenReturn(activeCards)

        val result = creditCardRepository.findActiveByUserId(userId)

        assertEquals(1, result.size)
        assertEquals(activeCards, result)
        verify(jpaRepository).findActiveByUserId(userId)
    }

    @Test
    fun `should check if credit card exists by id`() {
        val cardId = 1L

        whenever(jpaRepository.existsById(cardId)).thenReturn(true)

        val result = creditCardRepository.existsById(cardId)

        assertTrue(result)
        verify(jpaRepository).existsById(cardId)
    }

    @Test
    fun `should return false when credit card does not exist`() {
        val cardId = 999L

        whenever(jpaRepository.existsById(cardId)).thenReturn(false)

        val result = creditCardRepository.existsById(cardId)

        assertFalse(result)
        verify(jpaRepository).existsById(cardId)
    }

    @Test
    fun `should delete credit card`() {
        val creditCard = CreditCard(
            id = 1L,
            userId = UUID.randomUUID(),
            cardNumber = "4111111111111111",
            cardHolderName = "Test User",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            status = CreditCardStatus.ACTIVE,
            brand = "VISA"
        )

        doNothing().whenever(jpaRepository).delete(creditCard)

        creditCardRepository.delete(creditCard)

        verify(jpaRepository).delete(creditCard)
    }
}
