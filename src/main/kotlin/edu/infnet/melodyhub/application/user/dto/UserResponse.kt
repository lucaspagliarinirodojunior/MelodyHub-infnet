package edu.infnet.melodyhub.application.user.dto

import edu.infnet.melodyhub.domain.user.User
import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID?,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromUser(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                name = user.name,
                email = user.email,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
