package edu.infnet.melodyhub.domain.user

import edu.infnet.melodyhub.domain.events.UserSubscriptionUpgradedEvent
import edu.infnet.melodyhub.domain.shared.AggregateRoot
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

/**
 * Aggregate Root: Usuário (Account Context).
 *
 * Responsabilidades:
 * - Gerenciar dados e credenciais do usuário
 * - Controlar role/plano de assinatura
 * - Publicar eventos quando assinatura muda
 */
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
) : AggregateRoot() {

    /**
     * Atualiza role do usuário e publica evento.
     * Usado quando assinatura é adquirida via transação aprovada.
     *
     * Domain Event: UserSubscriptionUpgradedEvent
     */
    fun upgradeSubscription(newRole: UserRole) {
        val userId = this.id ?: throw IllegalStateException("Cannot upgrade subscription of unsaved user")

        val previousRole = this.role
        this.role = newRole
        this.updatedAt = LocalDateTime.now()

        // ✅ User publica evento de upgrade
        registerEvent(
            UserSubscriptionUpgradedEvent(
                userId = userId,
                previousRole = previousRole,
                newRole = newRole
            )
        )
    }

    fun isAdmin(): Boolean = role == UserRole.ADMIN

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
