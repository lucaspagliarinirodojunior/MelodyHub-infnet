package edu.infnet.melodyhub.domain.shared

import edu.infnet.melodyhub.domain.events.DomainEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AggregateRootTest {

    // Classe de teste que estende AggregateRoot
    private class TestAggregate : AggregateRoot() {
        fun triggerEvent(event: DomainEvent) {
            registerEvent(event)
        }
    }

    // Evento de teste
    private data class TestEvent(
        override val eventId: UUID = UUID.randomUUID(),
        override val occurredOn: LocalDateTime = LocalDateTime.now(),
        override val eventType: String = "test.event",
        val data: String
    ) : DomainEvent

    @Test
    fun `should register and retrieve domain events`() {
        val aggregate = TestAggregate()
        val event1 = TestEvent(data = "Event 1")
        val event2 = TestEvent(data = "Event 2")

        aggregate.triggerEvent(event1)
        aggregate.triggerEvent(event2)

        val events = aggregate.getEvents()
        assertEquals(2, events.size)
        assertEquals(event1, events[0])
        assertEquals(event2, events[1])
    }

    @Test
    fun `should clear events after getting them`() {
        val aggregate = TestAggregate()
        val event = TestEvent(data = "Test Event")

        aggregate.triggerEvent(event)
        assertEquals(1, aggregate.getEvents().size)

        val clearedEvents = aggregate.getAndClearEvents()
        assertEquals(1, clearedEvents.size)
        assertEquals(event, clearedEvents[0])

        // Verifica que os eventos foram limpos
        assertEquals(0, aggregate.getEvents().size)
    }

    @Test
    fun `should not clear events when using getEvents`() {
        val aggregate = TestAggregate()
        val event = TestEvent(data = "Test Event")

        aggregate.triggerEvent(event)

        val events1 = aggregate.getEvents()
        val events2 = aggregate.getEvents()

        assertEquals(1, events1.size)
        assertEquals(1, events2.size)
    }

    @Test
    fun `should return empty list when no events registered`() {
        val aggregate = TestAggregate()

        assertEquals(0, aggregate.getEvents().size)
        assertEquals(0, aggregate.getAndClearEvents().size)
    }

    @Test
    fun `should allow multiple getAndClearEvents calls`() {
        val aggregate = TestAggregate()
        val event1 = TestEvent(data = "Event 1")
        val event2 = TestEvent(data = "Event 2")

        aggregate.triggerEvent(event1)
        val cleared1 = aggregate.getAndClearEvents()
        assertEquals(1, cleared1.size)

        aggregate.triggerEvent(event2)
        val cleared2 = aggregate.getAndClearEvents()
        assertEquals(1, cleared2.size)

        assertEquals(0, aggregate.getEvents().size)
    }
}
