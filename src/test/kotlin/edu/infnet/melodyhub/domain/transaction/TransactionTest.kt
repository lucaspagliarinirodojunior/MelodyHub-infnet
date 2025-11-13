package edu.infnet.melodyhub.domain.transaction

import edu.infnet.melodyhub.domain.events.FraudDetectedEvent
import edu.infnet.melodyhub.domain.events.TransactionApprovedEvent
import edu.infnet.melodyhub.domain.events.TransactionValidatedEvent
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class TransactionTest {

    @Test
    fun `should create transaction with PENDING status`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        assertEquals(TransactionStatus.PENDING, transaction.status)
        assertNull(transaction.fraudReason)
    }

    @Test
    fun `should approve pending transaction and register event`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        transaction.approve(UserRole.BASIC)

        assertEquals(TransactionStatus.APPROVED, transaction.status)
        val events = transaction.getEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is TransactionApprovedEvent)

        val event = events[0] as TransactionApprovedEvent
        assertEquals(SubscriptionType.BASIC, event.subscriptionType)
        assertEquals(UserRole.BASIC, event.newUserRole)
    }

    @Test
    fun `should throw exception when approving non-pending transaction`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        transaction.approve(UserRole.BASIC)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction.approve(UserRole.BASIC)
        }

        assertEquals("Only pending transactions can be approved", exception.message)
    }

    @Test
    fun `should reject pending transaction with reason and register fraud event`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        transaction.reject("Suspicious activity detected")

        assertEquals(TransactionStatus.REJECTED, transaction.status)
        assertEquals("Suspicious activity detected", transaction.fraudReason)

        val events = transaction.getEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is FraudDetectedEvent)

        val event = events[0] as FraudDetectedEvent
        assertEquals("Suspicious activity detected", event.fraudReason)
    }

    @Test
    fun `should throw exception when rejecting non-pending transaction`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        transaction.reject("Test reason")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction.reject("Another reason")
        }

        assertEquals("Only pending transactions can be rejected", exception.message)
    }

    @Test
    fun `should record validation event for approved transaction`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        transaction.recordValidation(isValid = true, reason = null)

        val events = transaction.getEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is TransactionValidatedEvent)

        val event = events[0] as TransactionValidatedEvent
        assertTrue(event.isValid)
        assertNull(event.fraudReason)
    }

    @Test
    fun `should record validation event for rejected transaction`() {
        val transaction = Transaction(
            userId = UUID.randomUUID(),
            amount = BigDecimal("9.90"),
            subscriptionType = SubscriptionType.BASIC,
            creditCardId = 1L
        )

        transaction.recordValidation(isValid = false, reason = "Fraud detected")

        val events = transaction.getEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is TransactionValidatedEvent)

        val event = events[0] as TransactionValidatedEvent
        assertFalse(event.isValid)
        assertEquals("Fraud detected", event.fraudReason)
    }

    @Test
    fun `SubscriptionType should have correct prices`() {
        assertEquals(BigDecimal("9.90"), SubscriptionType.BASIC.monthlyPrice)
        assertEquals(BigDecimal("19.90"), SubscriptionType.PREMIUM.monthlyPrice)
    }
}
