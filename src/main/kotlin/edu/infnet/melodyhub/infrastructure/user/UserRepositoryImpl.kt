package edu.infnet.melodyhub.infrastructure.user

import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserRepositoryImpl(
    private val jpaUserRepository: JpaUserRepository
) : UserRepository {

    override fun save(user: User): User {
        return jpaUserRepository.save(user)
    }

    override fun findById(id: UUID): User? {
        return jpaUserRepository.findById(id).orElse(null)
    }

    override fun findByEmail(email: String): User? {
        return jpaUserRepository.findByEmail(email)
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaUserRepository.existsByEmail(email)
    }

    override fun findAll(): List<User> {
        return jpaUserRepository.findAll()
    }

    override fun delete(user: User) {
        jpaUserRepository.delete(user)
    }
}
