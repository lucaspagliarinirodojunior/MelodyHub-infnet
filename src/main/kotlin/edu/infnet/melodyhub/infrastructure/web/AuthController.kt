package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.auth.AuthService
import edu.infnet.melodyhub.application.auth.dto.LoginRequest
import edu.infnet.melodyhub.application.auth.dto.LoginResponse
import edu.infnet.melodyhub.application.auth.dto.MeResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun me(@RequestHeader("Authorization") authHeader: String): ResponseEntity<MeResponse> {
        // Extrair token do header "Bearer {token}"
        val token = authHeader.removePrefix("Bearer ")
        val response = authService.me(token)
        return ResponseEntity.ok(response)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(ErrorResponse(ex.message ?: "Erro de autenticação"))
    }
}
