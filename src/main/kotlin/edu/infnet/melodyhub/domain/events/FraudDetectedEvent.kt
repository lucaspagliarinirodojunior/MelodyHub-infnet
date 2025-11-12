package edu.infnet.melodyhub.domain.events

import java.time.LocalDateTime
import java.util.*

/**
 * Evento crítico publicado quando fraude é detectada em uma transação.
 * Permite que sistemas de auditoria e alertas reajam imediatamente.
 */
data class FraudDetectedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredOn: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "antifraud.fraud.detected",

    val transactionId: UUID,
    val userId: UUID,
    val fraudReason: String,
    val violatedRules: List<String>
) : DomainEvent
