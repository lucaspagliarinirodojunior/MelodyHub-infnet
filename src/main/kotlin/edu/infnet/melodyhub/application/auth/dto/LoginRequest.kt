package edu.infnet.melodyhub.application.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail deve ser válido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    val password: String
)
