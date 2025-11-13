package edu.infnet.melodyhub.domain.events

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class DomainEventsTest {

    @Test
    fun `should create UserSubscriptionUpgradedEvent with correct data`() {
        val userId = UUID.randomUUID()
        val event = UserSubscriptionUpgradedEvent(
            userId = userId,
            previousRole = UserRole.SEM_PLANO,
            newRole = UserRole.BASIC
        )

        assertEquals(userId, event.userId)
        assertEquals(UserRole.SEM_PLANO, event.previousRole)
        assertEquals(UserRole.BASIC, event.newRole)
        assertEquals("account.user.subscription.upgraded", event.eventType)
        assertNotNull(event.eventId)
        assertNotNull(event.occurredOn)
    }

    @Test
    fun `should create TransactionApprovedEvent with correct data`() {
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = TransactionApprovedEvent(
            transactionId = transactionId,
            userId = userId,
            subscriptionType = SubscriptionType.PREMIUM,
            newUserRole = UserRole.PREMIUM
        )

        assertEquals(transactionId, event.transactionId)
        assertEquals(userId, event.userId)
        assertEquals(SubscriptionType.PREMIUM, event.subscriptionType)
        assertEquals(UserRole.PREMIUM, event.newUserRole)
        assertEquals("antifraud.transaction.approved", event.eventType)
        assertNotNull(event.eventId)
        assertNotNull(event.occurredOn)
    }

    @Test
    fun `should create FraudDetectedEvent with correct data`() {
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val fraudReason = "High frequency detected"
        val violatedRules = listOf("Rule 1", "Rule 2")

        val event = FraudDetectedEvent(
            transactionId = transactionId,
            userId = userId,
            fraudReason = fraudReason,
            violatedRules = violatedRules
        )

        assertEquals(transactionId, event.transactionId)
        assertEquals(userId, event.userId)
        assertEquals(fraudReason, event.fraudReason)
        assertEquals(violatedRules, event.violatedRules)
        assertEquals("antifraud.fraud.detected", event.eventType)
        assertNotNull(event.eventId)
        assertNotNull(event.occurredOn)
    }

    @Test
    fun `should create TransactionValidatedEvent with valid transaction`() {
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val amount = BigDecimal("9.90")

        val event = TransactionValidatedEvent(
            transactionId = transactionId,
            userId = userId,
            amount = amount,
            subscriptionType = SubscriptionType.BASIC,
            isValid = true,
            fraudReason = null
        )

        assertEquals(transactionId, event.transactionId)
        assertEquals(userId, event.userId)
        assertEquals(amount, event.amount)
        assertEquals(SubscriptionType.BASIC, event.subscriptionType)
        assertTrue(event.isValid)
        assertNull(event.fraudReason)
        assertEquals("antifraud.transaction.validated", event.eventType)
        assertNotNull(event.eventId)
        assertNotNull(event.occurredOn)
    }

    @Test
    fun `should create TransactionValidatedEvent with invalid transaction`() {
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val amount = BigDecimal("9.90")
        val fraudReason = "Suspicious activity"

        val event = TransactionValidatedEvent(
            transactionId = transactionId,
            userId = userId,
            amount = amount,
            subscriptionType = SubscriptionType.BASIC,
            isValid = false,
            fraudReason = fraudReason
        )

        assertFalse(event.isValid)
        assertEquals(fraudReason, event.fraudReason)
    }

    @Test
    fun `should generate unique event IDs for different events`() {
        val userId = UUID.randomUUID()
        val event1 = UserSubscriptionUpgradedEvent(
            userId = userId,
            previousRole = UserRole.SEM_PLANO,
            newRole = UserRole.BASIC
        )
        val event2 = UserSubscriptionUpgradedEvent(
            userId = userId,
            previousRole = UserRole.BASIC,
            newRole = UserRole.PREMIUM
        )

        assertNotEquals(event1.eventId, event2.eventId)
    }

    @Test
    fun `should support all subscription type upgrades`() {
        val userId = UUID.randomUUID()
        val transactionId = UUID.randomUUID()

        val basicEvent = TransactionApprovedEvent(
            transactionId = transactionId,
            userId = userId,
            subscriptionType = SubscriptionType.BASIC,
            newUserRole = UserRole.BASIC
        )

        val premiumEvent = TransactionApprovedEvent(
            transactionId = transactionId,
            userId = userId,
            subscriptionType = SubscriptionType.PREMIUM,
            newUserRole = UserRole.PREMIUM
        )

        assertEquals(SubscriptionType.BASIC, basicEvent.subscriptionType)
        assertEquals(UserRole.BASIC, basicEvent.newUserRole)
        assertEquals(SubscriptionType.PREMIUM, premiumEvent.subscriptionType)
        assertEquals(UserRole.PREMIUM, premiumEvent.newUserRole)
    }
}
