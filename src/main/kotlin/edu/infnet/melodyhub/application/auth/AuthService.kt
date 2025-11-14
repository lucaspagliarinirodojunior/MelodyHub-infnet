package edu.infnet.melodyhub.application.auth

import edu.infnet.melodyhub.application.auth.dto.LoginRequest
import edu.infnet.melodyhub.application.auth.dto.LoginResponse
import edu.infnet.melodyhub.application.auth.dto.MeResponse
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.infrastructure.observability.UserContextEnricher
import edu.infnet.melodyhub.infrastructure.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

/**
 * Application Layer - Authentication Use Cases
 *
 * Service responsible for authentication business logic.
 * Logs authentication events for security audit and observability.
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val userContextEnricher: UserContextEnricher
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()

    fun login(request: LoginRequest): LoginResponse {
        logger.info("Login attempt for email: {}", request.email)

        // Buscar usuário por email
        val user = userRepository.findByEmail(request.email)
            ?: run {
                logger.warn("Login failed: user not found with email: {}", request.email)
                throw IllegalArgumentException("Credenciais inválidas")
            }

        // Verificar senha
        if (!passwordEncoder.matches(request.password, user.password)) {
            logger.warn("Login failed: invalid password for email: {}", request.email)
            throw IllegalArgumentException("Credenciais inválidas")
        }

        // Verificar que o usuário tem ID (sempre deve ter após ser salvo)
        val userId = user.id ?: throw IllegalStateException("Usuário sem ID")

        // Enriquecer contexto de logging
        userContextEnricher.enrichWithUserContext(userId.toString(), user.email, user.role.name)

        // Gerar token JWT com role
        val token = jwtService.generateToken(userId, user.email, user.role.name)

        logger.info(
            "User logged in successfully: userId={}, email={}, role={}",
            userId,
            user.email,
            user.role.name
        )

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
            ?: run {
                logger.warn("Invalid token provided to /me endpoint")
                throw IllegalArgumentException("Token inválido")
            }

        // Buscar usuário
        val user = userRepository.findById(userId)
            ?: run {
                logger.warn("User not found for /me endpoint: userId={}", userId)
                throw IllegalArgumentException("Usuário não encontrado")
            }

        // Enriquecer contexto de logging
        userContextEnricher.enrichWithUserContext(userId.toString(), user.email, user.role.name)

        logger.debug("User info retrieved: userId={}, email={}", userId, user.email)

        return MeResponse.fromUser(user)
    }
}
