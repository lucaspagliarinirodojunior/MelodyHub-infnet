package edu.infnet.melodyhub.application.user

import edu.infnet.melodyhub.application.user.dto.CreateUserRequest
import edu.infnet.melodyhub.domain.playlist.Playlist
import edu.infnet.melodyhub.domain.playlist.PlaylistRepository
import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        userRepository = mock()
        playlistRepository = mock()
        userService = UserService(userRepository, playlistRepository)
    }

    @Test
    fun `should create user successfully`() {
        val request = CreateUserRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123"
        )

        val userId = UUID.randomUUID()
        val savedUser = User(
            id = userId,
            name = request.name,
            email = request.email,
            password = "hashedPassword",
            role = UserRole.SEM_PLANO
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(playlistRepository.save(any())).thenReturn(mock())

        val response = userService.createUser(request)

        assertEquals(userId, response.id)
        assertEquals(request.name, response.name)
        assertEquals(request.email, response.email)
        assertEquals(UserRole.SEM_PLANO, response.role)

        verify(userRepository).existsByEmail(request.email)
        verify(userRepository).save(any())
        verify(playlistRepository).save(argThat {
            this is Playlist && this.name == "Favoritos" && this.userId == userId
        })
    }

    @Test
    fun `should throw exception when email already exists`() {
        val request = CreateUserRequest(
            name = "Test User",
            email = "existing@example.com",
            password = "password123"
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(true)

        val exception = assertThrows<IllegalArgumentException> {
            userService.createUser(request)
        }

        assertEquals("E-mail já cadastrado: existing@example.com", exception.message)
        verify(userRepository).existsByEmail(request.email)
        verify(userRepository, never()).save(any())
        verify(playlistRepository, never()).save(any())
    }

    @Test
    fun `should create user with specified role`() {
        val request = CreateUserRequest(
            name = "Admin User",
            email = "admin@example.com",
            password = "password123",
            role = UserRole.ADMIN
        )

        val userId = UUID.randomUUID()
        val savedUser = User(
            id = userId,
            name = request.name,
            email = request.email,
            password = "hashedPassword",
            role = UserRole.ADMIN
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(playlistRepository.save(any())).thenReturn(mock())

        val response = userService.createUser(request)

        assertEquals(UserRole.ADMIN, response.role)
    }

    @Test
    fun `should get user by id successfully`() {
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        whenever(userRepository.findById(userId)).thenReturn(user)

        val response = userService.getUserById(userId)

        assertEquals(userId, response.id)
        assertEquals(user.name, response.name)
        assertEquals(user.email, response.email)
        verify(userRepository).findById(userId)
    }

    @Test
    fun `should throw exception when user not found by id`() {
        val userId = UUID.randomUUID()
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            userService.getUserById(userId)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado com ID"))
    }

    @Test
    fun `should get user by email successfully`() {
        val email = "test@example.com"
        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = email,
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        whenever(userRepository.findByEmail(email)).thenReturn(user)

        val response = userService.getUserByEmail(email)

        assertEquals(email, response.email)
        assertEquals(user.name, response.name)
        verify(userRepository).findByEmail(email)
    }

    @Test
    fun `should throw exception when user not found by email`() {
        val email = "nonexistent@example.com"
        whenever(userRepository.findByEmail(email)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            userService.getUserByEmail(email)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado com e-mail"))
    }

    @Test
    fun `should get all users`() {
        val users = listOf(
            User(
                id = UUID.randomUUID(),
                name = "User 1",
                email = "user1@example.com",
                password = "hashedPassword1",
                role = UserRole.BASIC
            ),
            User(
                id = UUID.randomUUID(),
                name = "User 2",
                email = "user2@example.com",
                password = "hashedPassword2",
                role = UserRole.PREMIUM
            )
        )

        whenever(userRepository.findAll()).thenReturn(users)

        val response = userService.getAllUsers()

        assertEquals(2, response.size)
        assertEquals("User 1", response[0].name)
        assertEquals("User 2", response[1].name)
        verify(userRepository).findAll()
    }

    @Test
    fun `should delete user successfully`() {
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        whenever(userRepository.findById(userId)).thenReturn(user)

        userService.deleteUser(userId)

        verify(userRepository).findById(userId)
        verify(userRepository).delete(user)
    }

    @Test
    fun `should throw exception when deleting non-existent user`() {
        val userId = UUID.randomUUID()
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            userService.deleteUser(userId)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado com ID"))
        verify(userRepository).findById(userId)
        verify(userRepository, never()).delete(any())
    }
}
