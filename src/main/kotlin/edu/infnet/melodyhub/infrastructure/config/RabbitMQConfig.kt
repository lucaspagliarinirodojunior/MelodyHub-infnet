package edu.infnet.melodyhub.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuração do RabbitMQ para publicação de Domain Events.
 *
 * Topologia:
 * - Exchange: melodyhub.events (Topic)
 * - Routing Keys: antifraud.transaction.* (para eventos do AntiFraud)
 * - Queues: Consumidores podem criar suas próprias queues e bindings
 */
@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME = "melodyhub.events"

        // Queues de exemplo (podem ser criadas por consumidores)
        const val FRAUD_DETECTION_QUEUE = "antifraud.fraud.detection"
        const val TRANSACTION_AUDIT_QUEUE = "antifraud.transaction.audit"
        const val ACCOUNT_SUBSCRIPTION_QUEUE = "account.subscription.updates"
    }

    /**
     * Exchange principal para eventos de domínio.
     * Tipo: Topic - permite roteamento flexível baseado em routing keys.
     */
    @Bean
    fun eventsExchange(): TopicExchange {
        return TopicExchange(EXCHANGE_NAME, true, false)
    }

    /**
     * Queue para detecção de fraudes.
     * Consumidores de auditoria podem se inscrever nesta queue.
     */
    @Bean
    fun fraudDetectionQueue(): Queue {
        return QueueBuilder.durable(FRAUD_DETECTION_QUEUE)
            .withArgument("x-message-ttl", 86400000) // 24 horas
            .build()
    }

    /**
     * Queue para auditoria de transações.
     * Armazena todos os eventos de transações para fins de compliance.
     */
    @Bean
    fun transactionAuditQueue(): Queue {
        return QueueBuilder.durable(TRANSACTION_AUDIT_QUEUE)
            .withArgument("x-message-ttl", 2592000000) // 30 dias
            .build()
    }

    /**
     * Binding: Fraudes detectadas vão para fraud detection queue.
     */
    @Bean
    fun fraudDetectionBinding(
        fraudDetectionQueue: Queue,
        eventsExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(fraudDetectionQueue)
            .to(eventsExchange)
            .with("antifraud.fraud.detected")
    }

    /**
     * Binding: Todos os eventos de transação vão para audit queue.
     */
    @Bean
    fun transactionAuditBinding(
        transactionAuditQueue: Queue,
        eventsExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(transactionAuditQueue)
            .to(eventsExchange)
            .with("antifraud.transaction.*")
    }

    /**
     * Queue para atualizações de assinatura (Account Context).
     * Consome eventos de transação aprovada para atualizar User.
     */
    @Bean
    fun accountSubscriptionQueue(): Queue {
        return QueueBuilder.durable(ACCOUNT_SUBSCRIPTION_QUEUE)
            .withArgument("x-message-ttl", 86400000) // 24 horas
            .build()
    }

    /**
     * Binding: TransactionApprovedEvent vai para Account Context.
     * Cross-context communication via events.
     */
    @Bean
    fun accountSubscriptionBinding(
        accountSubscriptionQueue: Queue,
        eventsExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(accountSubscriptionQueue)
            .to(eventsExchange)
            .with("antifraud.transaction.approved")
    }

    /**
     * Conversor JSON para serializar/deserializar eventos.
     * Usa Jackson para converter objetos Kotlin em JSON.
     */
    @Bean
    fun jsonMessageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    /**
     * Template do RabbitMQ configurado com conversor JSON.
     */
    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        jsonMessageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = jsonMessageConverter
        return template
    }
}
