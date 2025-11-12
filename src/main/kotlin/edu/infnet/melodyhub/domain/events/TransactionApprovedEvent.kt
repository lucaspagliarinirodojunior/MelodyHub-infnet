package edu.infnet.melodyhub.domain.events

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.user.UserRole
import java.time.LocalDateTime
import java.util.*

/**
 * Evento publicado quando uma transação é aprovada após passar pela validação antifraude.
 * Indica que o usuário teve seu plano atualizado.
 */
data class TransactionApprovedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredOn: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "antifraud.transaction.approved",

    val transactionId: UUID,
    val userId: UUID,
    val subscriptionType: SubscriptionType,
    val newUserRole: UserRole
) : DomainEvent
