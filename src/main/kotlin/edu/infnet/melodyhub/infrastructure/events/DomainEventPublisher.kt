package edu.infnet.melodyhub.infrastructure.events

import edu.infnet.melodyhub.domain.events.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/**
 * Publisher de eventos de domínio via RabbitMQ.
 * Responsável por publicar eventos de domínio em um exchange do RabbitMQ.
 */
@Component
class DomainEventPublisher(
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(DomainEventPublisher::class.java)

    companion object {
        const val EXCHANGE_NAME = "melodyhub.events"
    }

    /**
     * Publica um evento de domínio no exchange RabbitMQ.
     * A routing key é derivada do eventType do evento.
     *
     * @param event O evento de domínio a ser publicado
     */
    fun publish(event: DomainEvent) {
        try {
            val routingKey = event.eventType

            logger.info(
                "Publishing domain event: eventType={}, eventId={}, occurredOn={}",
                event.eventType,
                event.eventId,
                event.occurredOn
            )

            rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event)

            logger.info(
                "Domain event published successfully: eventType={}, eventId={}",
                event.eventType,
                event.eventId
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to publish domain event: eventType={}, eventId={}, error={}",
                event.eventType,
                event.eventId,
                e.message,
                e
            )
            // Em produção, você pode querer retentar ou salvar em um dead letter queue
            throw e
        }
    }

    /**
     * Publica múltiplos eventos de domínio.
     *
     * @param events Lista de eventos a serem publicados
     */
    fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
