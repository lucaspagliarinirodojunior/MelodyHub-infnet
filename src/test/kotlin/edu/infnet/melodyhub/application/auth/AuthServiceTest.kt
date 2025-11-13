package edu.infnet.melodyhub.application.auth

import edu.infnet.melodyhub.application.auth.dto.LoginRequest
import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.domain.user.UserRole
import edu.infnet.melodyhub.infrastructure.security.JwtService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var jwtService: JwtService
    private lateinit var authService: AuthService
    private val passwordEncoder = BCryptPasswordEncoder()

    @BeforeEach
    fun setup() {
        userRepository = mock()
        jwtService = mock()
        authService = AuthService(userRepository, jwtService)
    }

    @Test
    fun `should login successfully with valid credentials`() {
        val email = "test@example.com"
        val password = "password123"
        val hashedPassword = passwordEncoder.encode(password)
        val userId = UUID.randomUUID()
        val token = "jwt-token-here"

        val user = User(
            id = userId,
            name = "Test User",
            email = email,
            password = hashedPassword,
            role = UserRole.BASIC
        )

        whenever(userRepository.findByEmail(email)).thenReturn(user)
        whenever(jwtService.generateToken(userId, email, UserRole.BASIC.name)).thenReturn(token)

        val request = LoginRequest(email = email, password = password)
        val response = authService.login(request)

        assertEquals(userId, response.userId)
        assertEquals("Test User", response.name)
        assertEquals(email, response.email)
        assertEquals(UserRole.BASIC, response.role)
        assertEquals(token, response.token)

        verify(userRepository).findByEmail(email)
        verify(jwtService).generateToken(userId, email, UserRole.BASIC.name)
    }

    @Test
    fun `should throw exception when user not found`() {
        val email = "nonexistent@example.com"
        val password = "password123"

        whenever(userRepository.findByEmail(email)).thenReturn(null)

        val request = LoginRequest(email = email, password = password)
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }

        assertEquals("Credenciais inválidas", exception.message)
        verify(userRepository).findByEmail(email)
        verify(jwtService, never()).generateToken(any(), any(), any())
    }

    @Test
    fun `should throw exception when password is invalid`() {
        val email = "test@example.com"
        val correctPassword = "correctPassword"
        val wrongPassword = "wrongPassword"
        val hashedPassword = passwordEncoder.encode(correctPassword)

        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = email,
            password = hashedPassword,
            role = UserRole.BASIC
        )

        whenever(userRepository.findByEmail(email)).thenReturn(user)

        val request = LoginRequest(email = email, password = wrongPassword)
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }

        assertEquals("Credenciais inválidas", exception.message)
        verify(userRepository).findByEmail(email)
        verify(jwtService, never()).generateToken(any(), any(), any())
    }

    @Test
    fun `should throw exception when user has no id`() {
        val email = "test@example.com"
        val password = "password123"
        val hashedPassword = passwordEncoder.encode(password)

        val user = User(
            id = null,
            name = "Test User",
            email = email,
            password = hashedPassword,
            role = UserRole.BASIC
        )

        whenever(userRepository.findByEmail(email)).thenReturn(user)

        val request = LoginRequest(email = email, password = password)
        val exception = assertThrows<IllegalStateException> {
            authService.login(request)
        }

        assertEquals("Usuário sem ID", exception.message)
    }

    @Test
    fun `should return user info from valid token`() {
        val userId = UUID.randomUUID()
        val token = "valid-jwt-token"

        val user = User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.PREMIUM
        )

        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(user)

        val response = authService.me(token)

        assertEquals(userId, response.userId)
        assertEquals("Test User", response.name)
        assertEquals("test@example.com", response.email)
        assertEquals(UserRole.PREMIUM, response.role)

        verify(jwtService).getUserIdFromToken(token)
        verify(userRepository).findById(userId)
    }

    @Test
    fun `should throw exception when token is invalid`() {
        val token = "invalid-jwt-token"

        whenever(jwtService.getUserIdFromToken(token)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            authService.me(token)
        }

        assertEquals("Token inválido", exception.message)
        verify(jwtService).getUserIdFromToken(token)
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun `should throw exception when user not found by token`() {
        val userId = UUID.randomUUID()
        val token = "valid-jwt-token"

        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            authService.me(token)
        }

        assertEquals("Usuário não encontrado", exception.message)
        verify(jwtService).getUserIdFromToken(token)
        verify(userRepository).findById(userId)
    }
}
