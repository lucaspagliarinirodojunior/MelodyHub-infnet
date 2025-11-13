package edu.infnet.melodyhub.domain.user

import edu.infnet.melodyhub.domain.events.UserSubscriptionUpgradedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class UserTest {

    @Test
    fun `should create user with default role SEM_PLANO`() {
        val user = User(
            name = "Test User",
            email = "test@example.com",
            password = "password123"
        )

        assertEquals("Test User", user.name)
        assertEquals("test@example.com", user.email)
        assertEquals(UserRole.SEM_PLANO, user.role)
    }

    @Test
    fun `should upgrade subscription and register event`() {
        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            role = UserRole.SEM_PLANO
        )

        user.upgradeSubscription(UserRole.PREMIUM)

        assertEquals(UserRole.PREMIUM, user.role)
        val events = user.getEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is UserSubscriptionUpgradedEvent)

        val event = events[0] as UserSubscriptionUpgradedEvent
        assertEquals(UserRole.SEM_PLANO, event.previousRole)
        assertEquals(UserRole.PREMIUM, event.newRole)
    }

    @Test
    fun `should throw exception when upgrading subscription of unsaved user`() {
        val user = User(
            name = "Test User",
            email = "test@example.com",
            password = "password123"
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            user.upgradeSubscription(UserRole.PREMIUM)
        }

        assertEquals("Cannot upgrade subscription of unsaved user", exception.message)
    }

    @Test
    fun `should identify admin user`() {
        val adminUser = User(
            name = "Admin",
            email = "admin@example.com",
            password = "password123",
            role = UserRole.ADMIN
        )

        val regularUser = User(
            name = "User",
            email = "user@example.com",
            password = "password123",
            role = UserRole.BASIC
        )

        assertTrue(adminUser.isAdmin())
        assertFalse(regularUser.isAdmin())
    }

    @Test
    fun `should clear events after getting them`() {
        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = "test@example.com",
            password = "password123"
        )

        user.upgradeSubscription(UserRole.PREMIUM)
        assertEquals(1, user.getEvents().size)

        val clearedEvents = user.getAndClearEvents()
        assertEquals(1, clearedEvents.size)
        assertEquals(0, user.getEvents().size)
    }

    @Test
    fun `should maintain equality based on id`() {
        val id = UUID.randomUUID()
        val user1 = User(
            id = id,
            name = "User 1",
            email = "user1@example.com",
            password = "password123"
        )
        val user2 = User(
            id = id,
            name = "User 2",
            email = "user2@example.com",
            password = "password456"
        )

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }
}
