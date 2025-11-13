package edu.infnet.melodyhub.domain.transaction

import edu.infnet.melodyhub.domain.events.FraudDetectedEvent
import edu.infnet.melodyhub.domain.events.TransactionApprovedEvent
import edu.infnet.melodyhub.domain.events.TransactionValidatedEvent
import edu.infnet.melodyhub.domain.shared.AggregateRoot
import edu.infnet.melodyhub.domain.user.UserRole
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Aggregate Root: Transação de assinatura (Payment Context).
 *
 * Responsabilidades:
 * - Gerenciar ciclo de vida da transação (PENDING → APPROVED/REJECTED)
 * - Publicar Domain Events quando mudanças ocorrem
 * - Proteger invariantes de negócio
 */
@Entity
@Table(name = "transactions")
class Transaction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val subscriptionType: SubscriptionType,

    @Column(nullable = false)
    val creditCardId: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TransactionStatus = TransactionStatus.PENDING,

    @Column
    var fraudReason: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AggregateRoot() {

    /**
     * Aprova a transação e registra evento de aprovação.
     *
     * Domain Event: TransactionApprovedEvent
     * Será consumido pelo Account Context para atualizar role do usuário.
     */
    fun approve(newUserRole: UserRole) {
        require(status == TransactionStatus.PENDING) {
            "Only pending transactions can be approved"
        }
        status = TransactionStatus.APPROVED
        updatedAt = LocalDateTime.now()

        // ✅ Aggregate publica seu próprio evento
        registerEvent(
            TransactionApprovedEvent(
                transactionId = id,
                userId = userId,
                subscriptionType = subscriptionType,
                newUserRole = newUserRole
            )
        )
    }

    /**
     * Rejeita a transação por fraude e registra evento.
     *
     * Domain Event: FraudDetectedEvent
     * Pode ser consumido por sistemas de auditoria/alertas.
     */
    fun reject(reason: String) {
        require(status == TransactionStatus.PENDING) {
            "Only pending transactions can be rejected"
        }
        status = TransactionStatus.REJECTED
        fraudReason = reason
        updatedAt = LocalDateTime.now()

        registerEvent(
            FraudDetectedEvent(
                transactionId = id,
                userId = userId,
                fraudReason = reason,
                violatedRules = listOf(reason)
            )
        )
    }

    /**
     * Registra evento de validação (aprovada ou rejeitada).
     * Chamado após salvar a transação.
     */
    fun recordValidation(isValid: Boolean, reason: String?) {
        registerEvent(
            TransactionValidatedEvent(
                transactionId = id,
                userId = userId,
                amount = amount,
                subscriptionType = subscriptionType,
                isValid = isValid,
                fraudReason = reason
            )
        )
    }
}

enum class SubscriptionType(val monthlyPrice: BigDecimal) {
    BASIC(BigDecimal("9.90")),
    PREMIUM(BigDecimal("19.90"))
}

enum class TransactionStatus {
    PENDING,
    APPROVED,
    REJECTED
}
