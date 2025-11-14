package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.creditcard.CreateCreditCardDTO
import edu.infnet.melodyhub.application.creditcard.CreditCardResponseDTO
import edu.infnet.melodyhub.application.creditcard.CreditCardService
import edu.infnet.melodyhub.application.creditcard.UpdateCreditCardStatusDTO
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.util.*

class CreditCardControllerTest {

    private lateinit var creditCardService: CreditCardService
    private lateinit var creditCardController: CreditCardController

    @BeforeEach
    fun setup() {
        creditCardService = mock()
        creditCardController = CreditCardController(creditCardService)
    }

    private fun createCreditCardResponse(
        id: Long = 1L,
        userId: UUID = UUID.randomUUID(),
        cardNumber: String = "4111111111111111",
        status: CreditCardStatus = CreditCardStatus.ACTIVE
    ): CreditCardResponseDTO {
        return CreditCardResponseDTO(
            id = id,
            userId = userId,
            maskedCardNumber = "**** **** **** 1111",
            cardHolderName = "Test User",
            brand = "VISA",
            expirationMonth = 12,
            expirationYear = 2025,
            status = status,
            isExpired = false,
            isValid = true
        )
    }

    @Test
    fun `should create credit card successfully`() {
        val userId = UUID.randomUUID()
        val dto = CreateCreditCardDTO(
            userId = userId,
            cardNumber = "4111111111111111",
            cardHolderName = "Test User",
            cvv = "123",
            expirationMonth = 12,
            expirationYear = 2025
        )
        val response = createCreditCardResponse(userId = userId)

        whenever(creditCardService.create(dto)).thenReturn(response)

        val result = creditCardController.create(dto)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body)
        verify(creditCardService).create(dto)
    }

    @Test
    fun `should handle IllegalArgumentException when creating card`() {
        val userId = UUID.randomUUID()
        val dto = CreateCreditCardDTO(
            userId = userId,
            cardNumber = "1234567890123456",
            cardHolderName = "Test User",
            cvv = "123",
            expirationMonth = 12,
            expirationYear = 2025
        )

        whenever(creditCardService.create(dto)).thenThrow(IllegalArgumentException("Número de cartão inválido"))

        val result = creditCardController.create(dto)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertTrue(result.body is ErrorResponse)
    }

    @Test
    fun `should handle generic exception when creating card`() {
        val userId = UUID.randomUUID()
        val dto = CreateCreditCardDTO(
            userId = userId,
            cardNumber = "4111111111111111",
            cardHolderName = "Test User",
            cvv = "123",
            expirationMonth = 12,
            expirationYear = 2025
        )

        whenever(creditCardService.create(dto)).thenThrow(RuntimeException("Erro inesperado"))

        val result = creditCardController.create(dto)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.statusCode)
        assertTrue(result.body is ErrorResponse)
    }

    @Test
    fun `should find credit card by id`() {
        val cardId = 1L
        val response = createCreditCardResponse(id = cardId)

        whenever(creditCardService.findById(cardId)).thenReturn(response)

        val result = creditCardController.findById(cardId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(creditCardService).findById(cardId)
    }

    @Test
    fun `should handle NoSuchElementException when finding by id`() {
        val cardId = 999L

        whenever(creditCardService.findById(cardId)).thenThrow(NoSuchElementException("Cartão não encontrado"))

        val result = creditCardController.findById(cardId)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertTrue(result.body is ErrorResponse)
    }

    @Test
    fun `should find credit cards by user id`() {
        val userId = UUID.randomUUID()
        val cards = listOf(
            createCreditCardResponse(id = 1L, userId = userId),
            createCreditCardResponse(id = 2L, userId = userId)
        )

        whenever(creditCardService.findByUserId(userId)).thenReturn(cards)

        val result = creditCardController.findByUserId(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
        verify(creditCardService).findByUserId(userId)
    }

    @Test
    fun `should find active credit cards by user id`() {
        val userId = UUID.randomUUID()
        val cards = listOf(
            createCreditCardResponse(id = 1L, userId = userId, status = CreditCardStatus.ACTIVE)
        )

        whenever(creditCardService.findActiveByUserId(userId)).thenReturn(cards)

        val result = creditCardController.findActiveByUserId(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(1, result.body?.size)
        verify(creditCardService).findActiveByUserId(userId)
    }

    @Test
    fun `should update credit card status`() {
        val cardId = 1L
        val dto = UpdateCreditCardStatusDTO(CreditCardStatus.BLOCKED)
        val response = createCreditCardResponse(id = cardId, status = CreditCardStatus.BLOCKED)

        whenever(creditCardService.updateStatus(cardId, dto)).thenReturn(response)

        val result = creditCardController.updateStatus(cardId, dto)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(creditCardService).updateStatus(cardId, dto)
    }

    @Test
    fun `should handle NoSuchElementException when updating status`() {
        val cardId = 999L
        val dto = UpdateCreditCardStatusDTO(CreditCardStatus.BLOCKED)

        whenever(creditCardService.updateStatus(cardId, dto))
            .thenThrow(NoSuchElementException("Cartão não encontrado"))

        val result = creditCardController.updateStatus(cardId, dto)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertTrue(result.body is ErrorResponse)
    }

    @Test
    fun `should delete credit card`() {
        val cardId = 1L

        doNothing().whenever(creditCardService).delete(cardId)

        val result = creditCardController.delete(cardId)

        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
        verify(creditCardService).delete(cardId)
    }

    @Test
    fun `should handle NoSuchElementException when deleting`() {
        val cardId = 999L

        whenever(creditCardService.delete(cardId)).thenThrow(NoSuchElementException("Cartão não encontrado"))

        val result = creditCardController.delete(cardId)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertTrue(result.body is ErrorResponse)
    }
}
