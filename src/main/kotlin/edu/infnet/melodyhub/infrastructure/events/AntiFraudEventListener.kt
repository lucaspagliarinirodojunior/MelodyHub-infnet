package edu.infnet.melodyhub.infrastructure.events

import edu.infnet.melodyhub.domain.events.FraudDetectedEvent
import edu.infnet.melodyhub.domain.events.TransactionApprovedEvent
import edu.infnet.melodyhub.domain.events.TransactionValidatedEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * Listener de exemplo para demonstrar consumo de Domain Events.
 * Em um sistema real, este seria um serviço separado ou microsserviço.
 *
 * Demonstra como outros bounded contexts podem reagir aos eventos do AntiFraud.
 */
@Component
class AntiFraudEventListener {
    private val logger = LoggerFactory.getLogger(AntiFraudEventListener::class.java)

    /**
     * Listener para eventos de fraude detectada.
     * Pode ser usado para:
     * - Enviar alertas para equipe de segurança
     * - Registrar em sistema de auditoria
     * - Atualizar métricas de fraude
     * - Bloquear usuário temporariamente
     */
    @RabbitListener(queues = ["antifraud.fraud.detection"])
    fun handleFraudDetected(event: FraudDetectedEvent) {
        logger.warn(
            "🚨 FRAUD DETECTED - Transaction: {}, User: {}, Reason: {}",
            event.transactionId,
            event.userId,
            event.fraudReason
        )

        // Aqui você poderia:
        // - Enviar notificação via email/SMS
        // - Gravar no banco de auditoria
        // - Incrementar contador de tentativas de fraude
        // - Disparar processo de revisão manual
    }

    /**
     * Listener para todos os eventos de transação (audit trail).
     * Mantém histórico completo de todas as validações.
     */
    @RabbitListener(queues = ["antifraud.transaction.audit"])
    fun handleTransactionValidated(event: TransactionValidatedEvent) {
        val status = if (event.isValid) "APPROVED ✅" else "REJECTED ❌"

        logger.info(
            "📝 TRANSACTION VALIDATED - Transaction: {}, User: {}, Amount: {}, Status: {}, Reason: {}",
            event.transactionId,
            event.userId,
            event.amount,
            status,
            event.fraudReason ?: "N/A"
        )

        // Aqui você poderia:
        // - Gravar em banco de auditoria
        // - Enviar para data lake/analytics
        // - Atualizar dashboard de métricas
    }

    /**
     * Listener para transações aprovadas.
     * Pode ser usado para processos pós-aprovação.
     */
    fun handleTransactionApproved(event: TransactionApprovedEvent) {
        logger.info(
            "✅ TRANSACTION APPROVED - Transaction: {}, User: {}, New Role: {}",
            event.transactionId,
            event.userId,
            event.newUserRole
        )

        // Aqui você poderia:
        // - Enviar email de boas-vindas ao plano
        // - Ativar benefícios do plano
        // - Atualizar sistema de CRM
        // - Disparar onboarding
    }
}
