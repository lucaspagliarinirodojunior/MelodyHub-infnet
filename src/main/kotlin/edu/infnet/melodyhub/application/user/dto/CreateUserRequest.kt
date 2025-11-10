package edu.infnet.melodyhub.application.user.dto

import edu.infnet.melodyhub.domain.user.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    val name: String,

    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail deve ser válido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String,

    val role: UserRole? = null // Opcional, padrão será SEM_PLANO
)
