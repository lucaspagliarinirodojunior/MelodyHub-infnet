package edu.infnet.melodyhub.application.auth.dto

import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRole
import java.util.UUID

data class MeResponse(
    val userId: UUID,
    val name: String,
    val email: String,
    val role: UserRole
) {
    companion object {
        fun fromUser(user: User): MeResponse {
            return MeResponse(
                userId = user.id!!,
                name = user.name,
                email = user.email,
                role = user.role
            )
        }
    }
}
