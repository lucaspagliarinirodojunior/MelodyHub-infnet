package edu.infnet.melodyhub.infrastructure.observability

import edu.infnet.melodyhub.domain.events.*
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * Infrastructure Layer - Observability
 *
 * Listener responsible for logging domain events for observability purposes.
 * This component subscribes to domain events and creates structured logs
 * that can be analyzed in Kibana.
 *
 * Following DDD principles:
 * - Domain events represent important business occurrences
 * - This infrastructure component observes (but doesn't modify) domain events
 * - Logging is a technical concern, properly separated from domain logic
 */
@Component
class DomainEventLogger(
    private val userContextEnricher: UserContextEnricher
) {

    private val logger = LoggerFactory.getLogger(DomainEventLogger::class.java)

    @RabbitListener(queues = ["transaction.approved.queue"])
    fun onTransactionApproved(event: TransactionApprovedEvent) {
        userContextEnricher.enrichWithEventContext("TransactionApproved")
        userContextEnricher.enrichWithTransactionContext(event.transactionId.toString())

        logger.info(
            "Domain Event: TransactionApproved - transactionId={}, userId={}, subscriptionType={}, newRole={}",
            event.transactionId,
            event.userId,
            event.subscriptionType,
            event.newUserRole
        )
    }

    @RabbitListener(queues = ["fraud.detected.queue"])
    fun onFraudDetected(event: FraudDetectedEvent) {
        userContextEnricher.enrichWithEventContext("FraudDetected")
        userContextEnricher.enrichWithTransactionContext(event.transactionId.toString())

        logger.warn(
            "Domain Event: FraudDetected - transactionId={}, userId={}, fraudReason={}",
            event.transactionId,
            event.userId,
            event.fraudReason
        )
    }

    @RabbitListener(queues = ["user.subscription.upgraded.queue"])
    fun onUserSubscriptionUpgraded(event: UserSubscriptionUpgradedEvent) {
        userContextEnricher.enrichWithEventContext("UserSubscriptionUpgraded")

        logger.info(
            "Domain Event: UserSubscriptionUpgraded - userId={}, previousRole={}, newRole={}",
            event.userId,
            event.previousRole,
            event.newRole
        )
    }

    @RabbitListener(queues = ["transaction.validated.queue"])
    fun onTransactionValidated(event: TransactionValidatedEvent) {
        userContextEnricher.enrichWithEventContext("TransactionValidated")
        userContextEnricher.enrichWithTransactionContext(event.transactionId.toString())

        logger.info(
            "Domain Event: TransactionValidated - transactionId={}, userId={}, isValid={}, fraudReason={}",
            event.transactionId,
            event.userId,
            event.isValid,
            event.fraudReason ?: "N/A"
        )
    }
}
