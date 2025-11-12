package edu.infnet.melodyhub.domain.events

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Evento publicado quando uma transação é validada pelo AntiFraud.
 * Este evento indica o resultado da validação (aprovada ou rejeitada).
 */
data class TransactionValidatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredOn: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "antifraud.transaction.validated",

    val transactionId: UUID,
    val userId: UUID,
    val amount: BigDecimal,
    val subscriptionType: SubscriptionType,
    val isValid: Boolean,
    val fraudReason: String?
) : DomainEvent
