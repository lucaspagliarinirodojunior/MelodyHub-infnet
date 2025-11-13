package edu.infnet.melodyhub.domain.shared

import edu.infnet.melodyhub.domain.events.DomainEvent
import jakarta.persistence.Transient

/**
 * Base class para Aggregates que publicam Domain Events.
 *
 * Pattern: Aggregate coleciona eventos internamente e os expõe
 * para que a camada de aplicação possa publicá-los após persistir
 * o aggregate com sucesso.
 *
 * Benefícios:
 * - Aggregate encapsula quando e quais eventos são criados
 * - Service não precisa saber detalhes dos eventos
 * - Eventos são publicados apenas após commit bem-sucedido
 *
 * Exemplo de uso:
 * ```
 * class Transaction : AggregateRoot() {
 *     fun approve() {
 *         status = APPROVED
 *         registerEvent(TransactionApprovedEvent(...))
 *     }
 * }
 * ```
 */
abstract class AggregateRoot {

    /**
     * Eventos de domínio coletados pelo aggregate.
     * @Transient: Não persiste no banco, apenas mantém em memória.
     */
    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()

    /**
     * Registra um evento de domínio.
     * Chamado internamente pelos métodos de negócio do aggregate.
     */
    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    /**
     * Retorna e limpa todos os eventos coletados.
     * Chamado pela camada de aplicação após persistir o aggregate.
     */
    fun getAndClearEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }

    /**
     * Retorna eventos sem limpar (para consulta).
     */
    fun getEvents(): List<DomainEvent> = domainEvents.toList()
}
