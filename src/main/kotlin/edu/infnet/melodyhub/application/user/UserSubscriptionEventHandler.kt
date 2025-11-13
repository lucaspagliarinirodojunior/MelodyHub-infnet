package edu.infnet.melodyhub.application.user

import edu.infnet.melodyhub.domain.events.TransactionApprovedEvent
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.infrastructure.events.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Event Handler para eventos de transação no contexto Account.
 *
 * Este handler é responsável por reagir a eventos do Payment Context
 * e atualizar o User no Account Context, implementando comunicação
 * event-driven entre bounded contexts.
 *
 * Pattern: Event-Driven Communication entre Aggregates
 * - Payment Context publica TransactionApprovedEvent
 * - Account Context reage e atualiza User
 * - Desacoplamento total entre contextos
 */
@Component
class UserSubscriptionEventHandler(
    private val userRepository: UserRepository,
    private val eventPublisher: DomainEventPublisher
) {
    private val logger = LoggerFactory.getLogger(UserSubscriptionEventHandler::class.java)

    /**
     * Reage ao evento de transação aprovada.
     *
     * Queue: account.subscription.updates
     * Routing Key: antifraud.transaction.approved
     *
     * Fluxo:
     * 1. Payment Context aprova transação
     * 2. Transaction aggregate publica TransactionApprovedEvent
     * 3. Este handler recebe o evento via RabbitMQ
     * 4. Account Context atualiza role do User
     * 5. User aggregate publica UserSubscriptionUpgradedEvent
     */
    @RabbitListener(queues = ["account.subscription.updates"])
    @Transactional
    fun handleTransactionApproved(event: TransactionApprovedEvent) {
        logger.info(
            "📨 Received TransactionApprovedEvent - Transaction: {}, User: {}, NewRole: {}",
            event.transactionId,
            event.userId,
            event.newUserRole
        )

        try {
            // Buscar usuário
            val user = userRepository.findById(event.userId)
                ?: throw IllegalArgumentException("User not found with ID: ${event.userId}")

            logger.info(
                "👤 Updating user subscription - User: {}, From: {} To: {}",
                event.userId,
                user.role,
                event.newUserRole
            )

            // ✅ User aggregate publica seu próprio evento ao fazer upgrade
            user.upgradeSubscription(event.newUserRole)

            // Salvar usuário
            val savedUser = userRepository.save(user)

            // Publicar eventos do aggregate
            savedUser.getAndClearEvents().forEach { userEvent ->
                eventPublisher.publish(userEvent)
            }

            logger.info(
                "✅ User subscription upgraded successfully - User: {}, NewRole: {}",
                event.userId,
                event.newUserRole
            )

        } catch (e: Exception) {
            logger.error(
                "❌ Failed to handle TransactionApprovedEvent - Transaction: {}, User: {}, Error: {}",
                event.transactionId,
                event.userId,
                e.message,
                e
            )
            // Em produção: requeue ou enviar para DLQ (Dead Letter Queue)
            throw e
        }
    }
}
