package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.user.UserService
import edu.infnet.melodyhub.application.user.dto.CreateUserRequest
import edu.infnet.melodyhub.application.user.dto.UserResponse
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.util.*

class UserControllerTest {

    private lateinit var userService: UserService
    private lateinit var userController: UserController

    @BeforeEach
    fun setup() {
        userService = mock()
        userController = UserController(userService)
    }

    private fun createUserResponse(
        id: UUID = UUID.randomUUID(),
        name: String = "Test User",
        email: String = "test@example.com",
        role: UserRole = UserRole.SEM_PLANO
    ): UserResponse {
        return UserResponse(
            id = id,
            name = name,
            email = email,
            role = role,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
    }

    @Test
    fun `should create user successfully`() {
        val request = CreateUserRequest("Test User", "test@example.com", "senha123")
        val userId = UUID.randomUUID()
        val response = createUserResponse(userId, "Test User", "test@example.com")

        whenever(userService.createUser(request)).thenReturn(response)

        val result = userController.createUser(request)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body)
        verify(userService).createUser(request)
    }

    @Test
    fun `should get user by id`() {
        val userId = UUID.randomUUID()
        val response = createUserResponse(userId)

        whenever(userService.getUserById(userId)).thenReturn(response)

        val result = userController.getUserById(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(userService).getUserById(userId)
    }

    @Test
    fun `should get all users`() {
        val users = listOf(
            createUserResponse(UUID.randomUUID(), "User 1", "user1@example.com"),
            createUserResponse(UUID.randomUUID(), "User 2", "user2@example.com")
        )

        whenever(userService.getAllUsers()).thenReturn(users)

        val result = userController.getAllUsers()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
        verify(userService).getAllUsers()
    }

    @Test
    fun `should get user by email`() {
        val email = "test@example.com"
        val response = createUserResponse(email = email)

        whenever(userService.getUserByEmail(email)).thenReturn(response)

        val result = userController.getUserByEmail(email)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(userService).getUserByEmail(email)
    }

    @Test
    fun `should delete user`() {
        val userId = UUID.randomUUID()

        doNothing().whenever(userService).deleteUser(userId)

        val result = userController.deleteUser(userId)

        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
        assertNull(result.body)
        verify(userService).deleteUser(userId)
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        val exception = IllegalArgumentException("Email já existe")

        val result = userController.handleIllegalArgumentException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("Email já existe", result.body?.message)
    }

    @Test
    fun `should handle NoSuchElementException`() {
        val exception = NoSuchElementException("Usuário não encontrado")

        val result = userController.handleNoSuchElementException(exception)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertEquals("Usuário não encontrado", result.body?.message)
    }
}
