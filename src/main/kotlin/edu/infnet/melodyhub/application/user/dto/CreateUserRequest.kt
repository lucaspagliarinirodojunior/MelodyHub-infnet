package edu.infnet.melodyhub.application.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateUserRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    val name: String,

    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail deve ser válido")
    val email: String
)
