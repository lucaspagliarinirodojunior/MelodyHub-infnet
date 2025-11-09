package edu.infnet.melodyhub.domain.user

import java.util.UUID

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun findAll(): List<User>
    fun delete(user: User)
}
