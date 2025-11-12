package edu.infnet.melodyhub.application.user

import edu.infnet.melodyhub.application.user.dto.CreateUserRequest
import edu.infnet.melodyhub.application.user.dto.UserResponse
import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.domain.playlist.Playlist
import edu.infnet.melodyhub.domain.playlist.PlaylistRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val playlistRepository: PlaylistRepository
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        // Regra de negócio: não permitir e-mails duplicados
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("E-mail já cadastrado: ${request.email}")
        }

        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = request.role ?: edu.infnet.melodyhub.domain.user.UserRole.SEM_PLANO
        )

        val savedUser = userRepository.save(user)

        // Criar playlist de Favoritos automaticamente
        val favoritesPlaylist = Playlist(
            name = "Favoritos",
            description = "Minhas músicas favoritas",
            userId = savedUser.id!!,
            isDefault = true,
            isPrivate = true
        )
        playlistRepository.save(favoritesPlaylist)

        return UserResponse.fromUser(savedUser)
    }

    @Transactional(readOnly = true)
    fun getUserById(id: UUID): UserResponse {
        val user = userRepository.findById(id)
            ?: throw NoSuchElementException("Usuário não encontrado com ID: $id")
        return UserResponse.fromUser(user)
    }

    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            ?: throw NoSuchElementException("Usuário não encontrado com e-mail: $email")
        return UserResponse.fromUser(user)
    }

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { UserResponse.fromUser(it) }
    }

    @Transactional
    fun deleteUser(id: UUID) {
        val user = userRepository.findById(id)
            ?: throw NoSuchElementException("Usuário não encontrado com ID: $id")
        userRepository.delete(user)
    }
}
