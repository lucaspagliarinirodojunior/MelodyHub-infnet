package edu.infnet.melodyhub.domain.events

import java.time.LocalDateTime
import java.util.*

/**
 * Interface base para todos os eventos de domínio.
 * Domain Events representam algo que aconteceu no domínio e que outros contextos podem reagir.
 */
interface DomainEvent {
    val eventId: UUID
    val occurredOn: LocalDateTime
    val eventType: String
}
