package edu.infnet.melodyhub.infrastructure.user

import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.*

class UserRepositoryImplTest {

    private lateinit var jpaUserRepository: JpaUserRepository
    private lateinit var userRepository: UserRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaUserRepository = mock()
        userRepository = UserRepositoryImpl(jpaUserRepository)
    }

    @Test
    fun `should save user`() {
        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        whenever(jpaUserRepository.save(user)).thenReturn(user)

        val result = userRepository.save(user)

        assertEquals(user, result)
        verify(jpaUserRepository).save(user)
    }

    @Test
    fun `should find user by id`() {
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        whenever(jpaUserRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = userRepository.findById(userId)

        assertEquals(user, result)
        verify(jpaUserRepository).findById(userId)
    }

    @Test
    fun `should return null when user not found by id`() {
        val userId = UUID.randomUUID()

        whenever(jpaUserRepository.findById(userId)).thenReturn(Optional.empty())

        val result = userRepository.findById(userId)

        assertNull(result)
        verify(jpaUserRepository).findById(userId)
    }

    @Test
    fun `should find user by email`() {
        val email = "test@example.com"
        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = email,
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        whenever(jpaUserRepository.findByEmail(email)).thenReturn(user)

        val result = userRepository.findByEmail(email)

        assertEquals(user, result)
        verify(jpaUserRepository).findByEmail(email)
    }

    @Test
    fun `should check if email exists`() {
        val email = "test@example.com"

        whenever(jpaUserRepository.existsByEmail(email)).thenReturn(true)

        val result = userRepository.existsByEmail(email)

        assertTrue(result)
        verify(jpaUserRepository).existsByEmail(email)
    }

    @Test
    fun `should return false when email does not exist`() {
        val email = "nonexistent@example.com"

        whenever(jpaUserRepository.existsByEmail(email)).thenReturn(false)

        val result = userRepository.existsByEmail(email)

        assertFalse(result)
        verify(jpaUserRepository).existsByEmail(email)
    }

    @Test
    fun `should find all users`() {
        val users = listOf(
            User(UUID.randomUUID(), "User 1", "user1@example.com", "pass1", UserRole.BASIC),
            User(UUID.randomUUID(), "User 2", "user2@example.com", "pass2", UserRole.PREMIUM)
        )

        whenever(jpaUserRepository.findAll()).thenReturn(users)

        val result = userRepository.findAll()

        assertEquals(2, result.size)
        assertEquals(users, result)
        verify(jpaUserRepository).findAll()
    }

    @Test
    fun `should delete user`() {
        val user = User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = UserRole.BASIC
        )

        doNothing().whenever(jpaUserRepository).delete(user)

        userRepository.delete(user)

        verify(jpaUserRepository).delete(user)
    }
}
