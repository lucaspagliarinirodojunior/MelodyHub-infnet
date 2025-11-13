package edu.infnet.melodyhub.domain.creditcard

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class CreditCardTest {

    @Test
    fun `should validate card number using Luhn algorithm`() {
        // Números inválidos (não passam no algoritmo de Luhn)
        assertFalse(CreditCard.isValidCardNumber("1234567890123456"))
        assertFalse(CreditCard.isValidCardNumber("0000000000000000"))
        assertFalse(CreditCard.isValidCardNumber("9999999999999999"))

        // Formato inválido também deve retornar false
        assertFalse(CreditCard.isValidCardNumber("abc123"))
    }

    @Test
    fun `should reject invalid card number formats`() {
        assertFalse(CreditCard.isValidCardNumber("123")) // Muito curto
        assertFalse(CreditCard.isValidCardNumber("12345678901234567890")) // Muito longo
        assertFalse(CreditCard.isValidCardNumber("abcd1234efgh5678")) // Contém letras
        assertFalse(CreditCard.isValidCardNumber("")) // Vazio
    }

    @Test
    fun `should identify card brand correctly`() {
        assertEquals("VISA", CreditCard.identifyBrand("4532015112830366"))
        assertEquals("MASTERCARD", CreditCard.identifyBrand("5425233430109903"))
        assertEquals("AMEX", CreditCard.identifyBrand("374245455400126"))
        assertEquals("AMEX", CreditCard.identifyBrand("371449635398431"))
        assertEquals("DISCOVER", CreditCard.identifyBrand("6011111111111117"))
        assertEquals("DISCOVER", CreditCard.identifyBrand("6511111111111117"))
        assertEquals("JCB", CreditCard.identifyBrand("3530111333300000"))
        assertEquals("UNKNOWN", CreditCard.identifyBrand("9999999999999999"))
    }

    @Test
    fun `should mask card number keeping last 4 digits`() {
        assertEquals("**** **** **** 0366", CreditCard.maskCardNumber("4532015112830366"))
        assertEquals("**** **** **** 9903", CreditCard.maskCardNumber("5425233430109903"))
        assertEquals("**** **** **** 0126", CreditCard.maskCardNumber("374245455400126"))
        assertEquals("****", CreditCard.maskCardNumber("123"))
    }

    @Test
    fun `should handle card number with spaces and dashes in masking`() {
        assertEquals("**** **** **** 0366", CreditCard.maskCardNumber("4532-0151-1283-0366"))
        assertEquals("**** **** **** 9903", CreditCard.maskCardNumber("5425 2334 3010 9903"))
    }

    @Test
    fun `should detect expired card`() {
        val currentDate = YearMonth.now()
        val lastMonth = currentDate.minusMonths(1)

        val expiredCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "John Doe",
            expirationMonth = lastMonth.monthValue,
            expirationYear = lastMonth.year,
            cvv = "123",
            brand = "VISA"
        )

        assertTrue(expiredCard.isExpired())
    }

    @Test
    fun `should detect valid card`() {
        val currentDate = YearMonth.now()
        val nextYear = currentDate.plusYears(1)

        val validCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "John Doe",
            expirationMonth = nextYear.monthValue,
            expirationYear = nextYear.year,
            cvv = "123",
            brand = "VISA"
        )

        assertFalse(validCard.isExpired())
    }

    @Test
    fun `should check if card is active`() {
        val activeCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "John Doe",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            brand = "VISA",
            status = CreditCardStatus.ACTIVE
        )

        val inactiveCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 9903",
            cardHolderName = "Jane Doe",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "456",
            brand = "MASTERCARD",
            status = CreditCardStatus.INACTIVE
        )

        assertTrue(activeCard.isActive())
        assertFalse(inactiveCard.isActive())
    }

    @Test
    fun `should activate and deactivate card`() {
        val card = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "John Doe",
            expirationMonth = 12,
            expirationYear = 2025,
            cvv = "123",
            brand = "VISA",
            status = CreditCardStatus.INACTIVE
        )

        card.activate()
        assertEquals(CreditCardStatus.ACTIVE, card.status)

        card.deactivate()
        assertEquals(CreditCardStatus.INACTIVE, card.status)
    }

    @Test
    fun `should validate card considering status and expiration`() {
        val currentDate = YearMonth.now()
        val nextYear = currentDate.plusYears(1)
        val lastMonth = currentDate.minusMonths(1)

        val validCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 0366",
            cardHolderName = "John Doe",
            expirationMonth = nextYear.monthValue,
            expirationYear = nextYear.year,
            cvv = "123",
            brand = "VISA",
            status = CreditCardStatus.ACTIVE
        )

        val expiredCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 9903",
            cardHolderName = "Jane Doe",
            expirationMonth = lastMonth.monthValue,
            expirationYear = lastMonth.year,
            cvv = "456",
            brand = "MASTERCARD",
            status = CreditCardStatus.ACTIVE
        )

        val inactiveCard = CreditCard(
            userId = UUID.randomUUID(),
            cardNumber = "**** **** **** 1234",
            cardHolderName = "Bob Smith",
            expirationMonth = nextYear.monthValue,
            expirationYear = nextYear.year,
            cvv = "789",
            brand = "VISA",
            status = CreditCardStatus.INACTIVE
        )

        assertTrue(validCard.isValid())
        assertFalse(expiredCard.isValid())
        assertFalse(inactiveCard.isValid())
    }
}
