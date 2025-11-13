package edu.infnet.melodyhub.domain.events

import edu.infnet.melodyhub.domain.user.UserRole
import java.time.LocalDateTime
import java.util.*

/**
 * Evento publicado quando um usuário tem sua assinatura atualizada.
 * Publicado pelo Account Context após processar TransactionApprovedEvent.
 */
data class UserSubscriptionUpgradedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredOn: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "account.user.subscription.upgraded",

    val userId: UUID,
    val previousRole: UserRole,
    val newRole: UserRole
) : DomainEvent
