package edu.infnet.melodyhub.domain.user

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @field:NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    var name: String,

    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail deve ser válido")
    @Column(nullable = false, unique = true)
    var email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.SEM_PLANO,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun updateName(newName: String) {
        require(newName.isNotBlank()) { "Nome não pode ser vazio" }
        this.name = newName
        this.updatedAt = LocalDateTime.now()
    }

    fun updateEmail(newEmail: String) {
        require(newEmail.isNotBlank()) { "E-mail não pode ser vazio" }
        require(newEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            "E-mail deve ser válido"
        }
        this.email = newEmail
        this.updatedAt = LocalDateTime.now()
    }

    fun updateRole(newRole: UserRole) {
        this.role = newRole
        this.updatedAt = LocalDateTime.now()
    }

    fun isAdmin(): Boolean = role == UserRole.ADMIN
    fun hasPremiumAccess(): Boolean = role == UserRole.PREMIUM || role == UserRole.ADMIN
    fun hasBasicAccess(): Boolean = role == UserRole.BASIC || hasPremiumAccess()
    fun hasNoPlan(): Boolean = role == UserRole.SEM_PLANO

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "User(id=$id, name='$name', email='$email', createdAt=$createdAt)"
    }
}
