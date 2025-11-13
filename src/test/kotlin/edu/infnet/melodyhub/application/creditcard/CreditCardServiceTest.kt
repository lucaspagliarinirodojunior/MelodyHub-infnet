package edu.infnet.melodyhub.application.creditcard

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardRepository
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.YearMonth
import java.util.UUID

class CreditCardServiceTest {

    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var userRepository: UserRepository
    private lateinit var creditCardService: CreditCardService

    @BeforeEach
    fun setup() {
        creditCardRepository = mock()
        userRepository = mock()
        creditCardService = CreditCardService(creditCardRepository, userRepository)
    }

    private fun createValidUser(userId: UUID = UUID.randomUUID()): User {
        return User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.BASIC
        )
    }

    private fun createValidCreditCardDTO(userId: UUID = UUID.randomUUID()): CreateCreditCardDTO {
        val futureDate = YearMonth.now().plusYears(1)
        return CreateCreditCardDTO(
            userId = userId,
            cardNumber = "4532015112830366", // Válido pelo algoritmo de Luhn
            cardHolderName = "John Doe",
            expirationMonth = futureDate.monthValue,
            expirationYear = futureDate.year,
            cvv = "123"
        )
    }

    @Test
    fun `should create credit card successfully`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val dto = createValidCreditCardDTO(userId)

        val savedCard = CreditCard(
            id = 1L,
            userId = userId,
            cardNumber = "**** **** **** 0366",
            cardHolderName = "JOHN DOE",
            expirationMonth = dto.expirationMonth,
            expirationYear = dto.expirationYear,
            cvv = dto.cvv,
            brand = "VISA",
            status = CreditCardStatus.ACTIVE
        )

        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(creditCardRepository.save(any())).thenReturn(savedCard)

        val response = creditCardService.create(dto)

        assertNotNull(response)
        assertEquals(1L, response.id)
        assertEquals("VISA", response.brand)
        verify(userRepository).findById(userId)
        verify(creditCardRepository).save(any())
    }

    @Test
    fun `should throw exception when user not found`() {
        val userId = UUID.randomUUID()
        val dto = createValidCreditCardDTO(userId)

        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Usuário não encontrado", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for invalid card number`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val dto = createValidCreditCardDTO(userId).copy(cardNumber = "1234567890123456")

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Número de cartão inválido", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for invalid CVV`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val dto = createValidCreditCardDTO(userId).copy(cvv = "12")

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("CVV inválido", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for invalid expiration month`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val dto = createValidCreditCardDTO(userId).copy(expirationMonth = 13)

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Mês de expiração inválido", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for past year`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val dto = createValidCreditCardDTO(userId).copy(expirationYear = 2020)

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Ano de expiração inválido", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for expired card`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val lastMonth = YearMonth.now().minusMonths(1)
        val dto = createValidCreditCardDTO(userId).copy(
            expirationMonth = lastMonth.monthValue,
            expirationYear = lastMonth.year
        )

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Cartão expirado", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for short card holder name`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        val dto = createValidCreditCardDTO(userId).copy(cardHolderName = "AB")

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Nome do titular inválido", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should throw exception for unknown card brand`() {
        val userId = UUID.randomUUID()
        val user = createValidUser(userId)
        // Número que passa no Luhn mas não tem bandeira reconhecida (começa com 8)
        val dto = createValidCreditCardDTO(userId).copy(cardNumber = "8000000000000003")

        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            creditCardService.create(dto)
        }

        assertEquals("Bandeira do cartão não reconhecida", exception.message)
        verify(creditCardRepository, never()).save(any())
    }

    @Test
    fun `should find credit card by id`() {
        val cardId = 1L
        val card = CreditCard(
            id = cardId,
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "JOHN DOE",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            brand = "VISA"
        )

        whenever(creditCardRepository.findById(cardId)).thenReturn(card)

        val response = creditCardService.findById(cardId)

        assertEquals(cardId, response.id)
        verify(creditCardRepository).findById(cardId)
    }

    @Test
    fun `should throw exception when card not found`() {
        val cardId = 999L

        whenever(creditCardRepository.findById(cardId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            creditCardService.findById(cardId)
        }

        assertEquals("Cartão não encontrado", exception.message)
    }

    @Test
    fun `should delete credit card successfully`() {
        val cardId = 1L
        val card = CreditCard(
            id = cardId,
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "JOHN DOE",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            brand = "VISA"
        )

        whenever(creditCardRepository.findById(cardId)).thenReturn(card)

        creditCardService.delete(cardId)

        verify(creditCardRepository).findById(cardId)
        verify(creditCardRepository).delete(card)
    }

    @Test
    fun `should return true when user has active card`() {
        val userId = UUID.randomUUID()
        val futureDate = YearMonth.now().plusYears(1)
        val activeCard = CreditCard(
            id = 1L,
            userId = userId,
            cardNumber = "**** **** **** 0366",
            cardHolderName = "JOHN DOE",
            expirationMonth = futureDate.monthValue,
            expirationYear = futureDate.year,
            cvv = "123",
            brand = "VISA",
            status = CreditCardStatus.ACTIVE
        )

        whenever(creditCardRepository.findActiveByUserId(userId)).thenReturn(listOf(activeCard))

        val result = creditCardService.hasActiveCard(userId)

        assertTrue(result)
        verify(creditCardRepository).findActiveByUserId(userId)
    }

    @Test
    fun `should return false when user has no active card`() {
        val userId = UUID.randomUUID()

        whenever(creditCardRepository.findActiveByUserId(userId)).thenReturn(emptyList())

        val result = creditCardService.hasActiveCard(userId)

        assertFalse(result)
        verify(creditCardRepository).findActiveByUserId(userId)
    }
}
