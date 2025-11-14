package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.auth.AuthService
import edu.infnet.melodyhub.application.auth.dto.LoginRequest
import edu.infnet.melodyhub.application.auth.dto.LoginResponse
import edu.infnet.melodyhub.application.auth.dto.MeResponse
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.util.*

class AuthControllerTest {

    private lateinit var authService: AuthService
    private lateinit var authController: AuthController

    @BeforeEach
    fun setup() {
        authService = mock()
        authController = AuthController(authService)
    }

    @Test
    fun `should login successfully`() {
        val request = LoginRequest("test@example.com", "senha123")
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        val userId = UUID.randomUUID()
        val response = LoginResponse(token, userId, "Test User", UserRole.BASIC)

        whenever(authService.login(request)).thenReturn(response)

        val result = authController.login(request)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(authService).login(request)
    }

    @Test
    fun `should return user info from token`() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        val authHeader = "Bearer $token"
        val userId = UUID.randomUUID()
        val response = MeResponse(userId, "Test User", "test@example.com", UserRole.BASIC)

        whenever(authService.me(token)).thenReturn(response)

        val result = authController.me(authHeader)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(authService).me(token)
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        val exception = IllegalArgumentException("Credenciais inválidas")

        val result = authController.handleIllegalArgumentException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("Credenciais inválidas", result.body?.message)
    }
}
