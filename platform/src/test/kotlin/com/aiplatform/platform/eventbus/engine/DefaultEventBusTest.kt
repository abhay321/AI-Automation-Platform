package com.aiplatform.platform.eventbus.engine

import com.aiplatform.platform.eventbus.api.*
import com.aiplatform.platform.eventbus.bridge.EventBridge
import com.aiplatform.platform.logging.context.CorrelationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DefaultEventBusTest {

    private fun createEnvelope(
        eventName: String,
        payload: String,
        traceId: String? = "test-trace-id",
        correlationId: String? = "test-corr-id"
    ): EventEnvelope<String> {
        return EventEnvelope(
            id = UUID.randomUUID(),
            eventName = eventName,
            payload = payload,
            traceId = traceId,
            spanId = null,
            correlationId = correlationId
        )
    }

    @Test
    fun `test exact topic matching local delivery`() = runBlocking {
        val bus = DefaultEventBus()
        bus.onBootstrap()
        bus.onInitialize()
        bus.onStartup()

        val eventsReceived = CopyOnWriteArrayList<EventEnvelope<String>>()
        val latch = CountDownLatch(1)

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                eventsReceived.add(envelope)
                latch.countDown()
            }
        }

        bus.subscribe("orders.created", subscriber)
        bus.publish(createEnvelope("orders.created", "Order #123"))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals(1, eventsReceived.size)
        assertEquals("Order #123", eventsReceived[0].payload)

        bus.onShutdown()
        bus.onCleanup()
    }

    @Test
    fun `test single level wildcard matching`() = runBlocking {
        val bus = DefaultEventBus()
        val eventsReceived = CopyOnWriteArrayList<EventEnvelope<String>>()
        val latch = CountDownLatch(2)

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                eventsReceived.add(envelope)
                latch.countDown()
            }
        }

        bus.subscribe("orders.*.created", subscriber)

        // Matches
        bus.publish(createEnvelope("orders.us.created", "US Order"))
        bus.publish(createEnvelope("orders.eu.created", "EU Order"))

        // Does NOT match (too deep)
        bus.publish(createEnvelope("orders.us.retail.created", "US Retail Order"))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals(2, eventsReceived.size)
        assertTrue(eventsReceived.any { it.payload == "US Order" })
        assertTrue(eventsReceived.any { it.payload == "EU Order" })
        assertFalse(eventsReceived.any { it.payload == "US Retail Order" })
    }

    @Test
    fun `test recursive tail wildcard matching`() = runBlocking {
        val bus = DefaultEventBus()
        val eventsReceived = CopyOnWriteArrayList<EventEnvelope<String>>()
        val latch = CountDownLatch(3)

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                eventsReceived.add(envelope)
                latch.countDown()
            }
        }

        bus.subscribe("orders.>", subscriber)

        // All match
        bus.publish(createEnvelope("orders.created", "Simple Order"))
        bus.publish(createEnvelope("orders.us.created", "US Order"))
        bus.publish(createEnvelope("orders.us.retail.completed", "Deep Order"))

        // Does NOT match (different prefix)
        bus.publish(createEnvelope("inventory.updated", "Inventory"))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals(3, eventsReceived.size)
        assertFalse(eventsReceived.any { it.payload == "Inventory" })
    }

    @Test
    fun `test synchronous delivery blocks publisher`() {
        val bus = DefaultEventBus()
        val orderOfExecution = CopyOnWriteArrayList<String>()

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                delay(100)
                orderOfExecution.add("subscriber-completed")
            }
        }

        bus.subscribe("orders.created", subscriber)
        bus.publishSync(createEnvelope("orders.created", "Sync Order"))
        orderOfExecution.add("publisher-completed")

        assertEquals(listOf("subscriber-completed", "publisher-completed"), orderOfExecution)
    }

    @Test
    fun `test sticky events replay automatically`() = runBlocking {
        val bus = DefaultEventBus()
        
        // Publish sticky event BEFORE subscribing
        bus.publishSticky(createEnvelope("config.updated", "Sticky Payload"))

        val eventsReceived = CopyOnWriteArrayList<EventEnvelope<String>>()
        val latch = CountDownLatch(1)

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                eventsReceived.add(envelope)
                latch.countDown()
            }
        }

        // Subscribing should immediately trigger replay of sticky event
        bus.subscribe("config.>", subscriber)

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals(1, eventsReceived.size)
        assertEquals("Sticky Payload", eventsReceived[0].payload)
    }

    @Test
    fun `test delayed delivery delays correctly`() = runBlocking {
        val bus = DefaultEventBus()
        val eventsReceived = CopyOnWriteArrayList<EventEnvelope<String>>()
        val latch = CountDownLatch(1)

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                eventsReceived.add(envelope)
                latch.countDown()
            }
        }

        bus.subscribe("notification.send", subscriber)
        
        val start = System.currentTimeMillis()
        bus.publishDelayed(createEnvelope("notification.send", "Late Alert"), Duration.ofMillis(300))

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        val elapsed = System.currentTimeMillis() - start
        
        assertEquals(1, eventsReceived.size)
        assertTrue(elapsed >= 300, "Should have been delayed by at least 300ms, took $elapsed ms")
    }

    @Test
    fun `test asFlow publishes events to kotlin flow`() = runBlocking {
        val bus = DefaultEventBus()

        val flow = bus.asFlow<String>("orders.>")

        val deferredResult = async {
            flow.take(2).toList()
        }

        // Give reader flow a moment to register subscriber
        delay(50)

        bus.publish(createEnvelope("orders.us.created", "Order 1"))
        bus.publish(createEnvelope("orders.eu.created", "Order 2"))

        val results = deferredResult.await()
        assertEquals(2, results.size)
        assertEquals("Order 1", results[0].payload)
        assertEquals("Order 2", results[1].payload)
    }

    @Test
    fun `test context propagation maintains tracking headers`() = runBlocking {
        val bus = DefaultEventBus()
        val latch = CountDownLatch(1)
        var capturedTraceId: String? = null
        var capturedCorrelationId: String? = null

        val subscriber = object : EventSubscriber<String> {
            override suspend fun onEvent(envelope: EventEnvelope<String>) {
                capturedTraceId = CorrelationContext.traceId
                capturedCorrelationId = CorrelationContext.correlationId
                latch.countDown()
            }
        }

        bus.subscribe("tracing.test", subscriber)
        bus.publish(createEnvelope("tracing.test", "Check Trace", traceId = "my-custom-trace", correlationId = "my-custom-corr"))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals("my-custom-trace", capturedTraceId)
        assertEquals("my-custom-corr", capturedCorrelationId)
    }

    @Test
    fun `test external message bridging`() = runBlocking {
        val bus = DefaultEventBus()

        val bridgedEvents = CopyOnWriteArrayList<EventEnvelope<*>>()
        val mockBridge = object : EventBridge {
            override val targetName: String = "mock-rabbitmq"
            
            override fun shouldBridge(envelope: EventEnvelope<*>): Boolean {
                return envelope.eventName.startsWith("external.")
            }

            override suspend fun bridgeEvent(envelope: EventEnvelope<*>) {
                bridgedEvents.add(envelope)
            }
        }

        bus.registerBridge(mockBridge)
        assertEquals(1, bus.activeBridges.size)

        // This one should be bridged
        bus.publish(createEnvelope("external.orders.created", "Foreign Sale"))
        
        // This one should NOT be bridged
        bus.publish(createEnvelope("local.orders.created", "Local Sale"))

        // Give async dispatcher a moment
        delay(150)

        assertEquals(1, bridgedEvents.size)
        assertEquals("Foreign Sale", bridgedEvents[0].payload)

        bus.deregisterBridge(mockBridge)
        assertTrue(bus.activeBridges.isEmpty())
    }
}
