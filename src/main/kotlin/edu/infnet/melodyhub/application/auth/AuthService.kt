package edu.infnet.melodyhub.application.auth

import edu.infnet.melodyhub.application.auth.dto.LoginRequest
import edu.infnet.melodyhub.application.auth.dto.LoginResponse
import edu.infnet.melodyhub.application.auth.dto.MeResponse
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.infrastructure.security.JwtService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun login(request: LoginRequest): LoginResponse {
        // Buscar usuário por email
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Credenciais inválidas")

        // Verificar senha
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Credenciais inválidas")
        }

        // Verificar que o usuário tem ID (sempre deve ter após ser salvo)
        val userId = user.id ?: throw IllegalStateException("Usuário sem ID")

        // Gerar token JWT com role
        val token = jwtService.generateToken(userId, user.email, user.role.name)

        return LoginResponse(
            userId = userId,
            name = user.name,
            email = user.email,
            role = user.role,
            token = token
        )
    }

    fun me(token: String): MeResponse {
        // Extrair userId do token
        val userId = jwtService.getUserIdFromToken(token)
            ?: throw IllegalArgumentException("Token inválido")

        // Buscar usuário
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado")

        return MeResponse.fromUser(user)
    }
}
