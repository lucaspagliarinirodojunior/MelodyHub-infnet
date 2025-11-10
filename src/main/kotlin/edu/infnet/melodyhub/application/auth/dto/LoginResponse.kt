package edu.infnet.melodyhub.application.auth.dto

import edu.infnet.melodyhub.domain.user.UserRole
import java.util.UUID

data class LoginResponse(
    val userId: UUID,
    val name: String,
    val email: String,
    val role: UserRole,
    val token: String
)
