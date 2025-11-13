package edu.infnet.melodyhub.infrastructure.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        jwtService = JwtService()
    }

    @Test
    fun `should generate valid JWT token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val role = "BASIC"

        val token = jwtService.generateToken(userId, email, role)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `should validate valid token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val role = "BASIC"

        val token = jwtService.generateToken(userId, email, role)
        val isValid = jwtService.validateToken(token)

        assertTrue(isValid)
    }

    @Test
    fun `should not validate invalid token`() {
        val invalidToken = "invalid.jwt.token"

        val isValid = jwtService.validateToken(invalidToken)

        assertFalse(isValid)
    }

    @Test
    fun `should extract userId from token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val role = "BASIC"

        val token = jwtService.generateToken(userId, email, role)
        val extractedUserId = jwtService.getUserIdFromToken(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `should return null for invalid token when extracting userId`() {
        val invalidToken = "invalid.jwt.token"

        val extractedUserId = jwtService.getUserIdFromToken(invalidToken)

        assertNull(extractedUserId)
    }

    @Test
    fun `should extract email from token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val role = "BASIC"

        val token = jwtService.generateToken(userId, email, role)
        val extractedEmail = jwtService.getEmailFromToken(token)

        assertEquals(email, extractedEmail)
    }

    @Test
    fun `should return null for invalid token when extracting email`() {
        val invalidToken = "invalid.jwt.token"

        val extractedEmail = jwtService.getEmailFromToken(invalidToken)

        assertNull(extractedEmail)
    }

    @Test
    fun `should extract role from token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val role = "PREMIUM"

        val token = jwtService.generateToken(userId, email, role)
        val extractedRole = jwtService.getRoleFromToken(token)

        assertEquals(role, extractedRole)
    }

    @Test
    fun `should return null for invalid token when extracting role`() {
        val invalidToken = "invalid.jwt.token"

        val extractedRole = jwtService.getRoleFromToken(invalidToken)

        assertNull(extractedRole)
    }

    @Test
    fun `should generate different tokens for different users`() {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val email = "test@example.com"
        val role = "BASIC"

        val token1 = jwtService.generateToken(userId1, email, role)
        val token2 = jwtService.generateToken(userId2, email, role)

        assertNotEquals(token1, token2)
    }

    @Test
    fun `should handle all user roles`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = listOf("ADMIN", "PREMIUM", "BASIC", "SEM_PLANO")

        roles.forEach { role ->
            val token = jwtService.generateToken(userId, email, role)
            val extractedRole = jwtService.getRoleFromToken(token)

            assertEquals(role, extractedRole)
        }
    }
}
