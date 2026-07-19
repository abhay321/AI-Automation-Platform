# Enterprise Event Bus Architecture & Specification
Version: 1.0 (Event Bus Baseline - Frozen)

This document establishes the official architectural blueprint, package layout, type-safe API interfaces, propagation diagrams, transactional boundaries, and distributed bridge abstractions for the **AI Automation Platform Event Bus**.

The Event Bus serves as the primary reactive communication backbone of the platform, enabling highly decoupled, asynchronous, type-safe, and non-blocking interactions between the Runtime Kernel, Workflow Engine, AI Providers, and custom isolated Plugins.

---

## 1. Directory & Package Structure

The Event Bus package resides in the `platform` module under a dedicated, low-dependency namespace:

```
platform/
└── eventbus/
    ├── api/                 # Core Event Bus interfaces, subscriber handles, filter contracts
    ├── model/               # Standard system events (Lifecycle, Workflow, AI, Plugin, Audit)
    ├── priority/            # Event Priority definition and priority routing controls
    ├── tx/                  # Transactional outbox pattern and JDBC sync coordinators
    └── bridge/              # Message broker bridges (Kafka, RabbitMQ, EventBridge)
```

---

## 2. Event Routing & Lifecycle Flow Diagram

The diagram below details how events move through the Event Bus, tracking context preservation, validation, priority queuing, and async subscribers:

```
[ Producer ] ──► Emits Event (e.g. ai.prompt.completed)
     │
     ▼
┌────────────────────────────────────────────────────────┐
│ 1. Envelope Wrapping                                   │  ◄── Wraps payload with ID, Timestamp, Trace ID, etc.
└────────────────────────┬───────────────────────────────┘
                         ▼
┌────────────────────────────────────────────────────────┐
│ 2. Context Propagation                                 │  ◄── Inherits TraceId, SpanId from ObservabilityContext
└────────────────────────┬───────────────────────────────┘
                         ▼
┌────────────────────────────────────────────────────────┐
│ 3. Validation & Encryption                             │  ◄── Sanitizes fields, strips or encrypts PII metadata
└────────────────────────┬───────────────────────────────┘
                         ▼
┌────────────────────────────────────────────────────────┐
│ 4. Routing & Filter Dispatcher                        │  ◄── Matches wildcard patterns (e.g., "workflow.*.failed")
└────────────────────────┬───────────────────────────────┘
                         ├───────────────────────────────┐
                         ▼ (Immediate Sync)              ▼ (Asynchronous)
             ┌───────────────────────┐       ┌───────────────────────┐
             │ Synchronous Delivery  │       │ Priority RingBuffer   │  ◄── Sorts by Priority (HIGH / CRITICAL)
             │ (Same Thread Dispatch)│       └───────────┬───────────┘
             └───────────────────────┘                   ▼
                                             ┌───────────────────────┐
                                             │ Coroutine Dispatchers │  ◄── Non-blocking Channel workers
                                             └───────────┬───────────┘
                                                         ▼
                                             ┌───────────────────────┐
                                             │ Async Subscriber Loop │
                                             └───────────────────────┘
```

---

## 3. High-Fidelity API Specifications

To guarantee compile-time type-safety, zero framework coupling, and seamless coroutine execution, the Event Bus defines the following Kotlin API contracts:

### 3.1. Event Envelope Specification
Every event transmitted through the bus must be wrapped in a secure `EventEnvelope`, preserving routing metadata, compliance auditing markers, and observability spans:

```kotlin
package com.aiplatform.platform.eventbus.api

import java.time.Instant
import java.util.UUID

/**
 * Defines the priority of the event. Under high system load, events are processed
 * according to their priority queue ranking.
 */
enum class EventPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Type-safe, metadata-rich container wrapping all system event payloads.
 */
data class EventEnvelope<T : Any>(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val eventName: String,             // Ex: "workflow.node.started"
    val eventVersion: Int = 1,
    val payload: T,
    val priority: EventPriority = EventPriority.NORMAL,
    
    // Unified Observability Tracking
    val traceId: String?,
    val spanId: String?,
    val correlationId: String?,
    val userId: String? = null,
    val workspaceId: String? = null,
    
    // Routing Capabilities
    val isSticky: Boolean = false,
    val isReplayEnabled: Boolean = false,
    val metadata: Map<String, Any?> = emptyMap()
)
```

### 3.2. Core Event Bus Engine Contract
The central manager coordinating publishers and subscribers natively using Kotlin Coroutines:

```kotlin
package com.aiplatform.platform.eventbus.api

import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface EventSubscriber<T : Any> {
    suspend fun onEvent(envelope: EventEnvelope<T>)
}

interface SubscriptionHandle {
    /**
     * Unsubscribes the registered subscriber from the event loop, releasing all thread handles.
     */
    fun cancel()
}

interface EventBus {
    /**
     * Publishes an event asynchronously onto the bus. Processing begins immediately on coroutine dispatchers.
     */
    suspend fun <T : Any> publish(envelope: EventEnvelope<T>)

    /**
     * Publishes an event synchronously. The caller thread block-waits until all synchronous subscribers finish execution.
     * Crucial for transactional integrity and database hooks.
     */
    fun <T : Any> publishSync(envelope: EventEnvelope<T>)

    /**
     * Publishes an event with a configured delay duration. Natively backed by non-blocking Coroutine delay loops.
     */
    suspend fun <T : Any> publishDelayed(envelope: EventEnvelope<T>, delay: Duration)

    /**
     * Publishes a "Sticky Event". The bus retains the latest instance of this event type, immediately 
     * dispatching it to any new subscribers registering for matching topics.
     */
    suspend fun <T : Any> publishSticky(envelope: EventEnvelope<T>)

    /**
     * Registers a subscriber for a specific topic pattern. Supports wildcard structures:
     * - "workflow.test" (Exact Match)
     * - "workflow.*" (Single Level Wildcard)
     * - "workflow.>" (Multi-level Recursive Wildcard)
     */
    fun <T : Any> subscribe(topicPattern: String, subscriber: EventSubscriber<T>): SubscriptionHandle

    /**
     * Natively exposes event streams as Kotlin Flows for seamless reactive composition (operators like filter, map, zip).
     */
    fun <T : Any> asFlow(topicPattern: String): Flow<EventEnvelope<T>>

    /**
     * Clears cached sticky events matching a given pattern.
     */
    fun clearSticky(topicPattern: String)
}
```

### 3.3. Transactional Outbox Pattern Integration
To prevent inconsistencies where events are dispatched to external brokers but the local database transaction rolls back, we define the `TransactionalEventBus`:

```kotlin
package com.aiplatform.platform.eventbus.tx

import com.aiplatform.platform.eventbus.api.EventEnvelope

interface TransactionalEventBus {
    /**
     * Enqueues an event within the active SQL transaction. The event is committed to a persistent 
     * database Outbox table first. It is dispatched to the in-memory Event Bus ONLY after 
     * the transaction successfully commits.
     */
    suspend fun <T : Any> publishTransactional(envelope: EventEnvelope<T>)
}
```

### 3.4. Distributed Message Broker Bridge Abstraction
Provides absolute separation of concerns. The platform code interacts entirely with the local `EventBus`, while custom adapter plug-ins bridge events to enterprise message brokers:

```kotlin
package com.aiplatform.platform.eventbus.bridge

import com.aiplatform.platform.eventbus.api.EventEnvelope

interface EventBridge {
    /**
     * Name of the target integration broker (e.g., "Kafka-Cluster-1", "RabbitMQ-Exchange").
     */
    val targetName: String

    /**
     * Determines whether this bridge handles a given event category.
     */
    fun shouldBridge(envelope: EventEnvelope<*>): Boolean

    /**
     * Bridges an envelope to the external broker.
     */
    suspend fun bridgeEvent(envelope: EventEnvelope<*>)
}

interface EventBridgeRegistry {
    fun registerBridge(bridge: EventBridge)
    fun deregisterBridge(bridge: EventBridge)
    val activeBridges: List<EventBridge>
}
```

---

## 4. Platform Event Taxonomy Model Specifications

The Event Bus organizes message structures into formalized domains:

### 1. Lifecycle Events
* `platform.lifecycle.bootstrap.started`: Broadcasted when the bootloader starts loading configurations.
* `platform.lifecycle.ready`: Fired when all modules pass readiness checks and the API socket is bound.
* `platform.lifecycle.shutdown.initiated`: Emitted immediately upon receiving SIGTERM, allowing active processing to begin connection draining.

### 2. Workflow Events
* `workflow.execution.started`: Emitted when a user DAG begins running. Includes workspace and execution IDs.
* `workflow.node.started` / `workflow.node.completed`: Fine-grained tracking of specific executor nodes.
* `workflow.execution.failed`: Sent if a node throws an unrecoverable exception, detailing stack traces and rollback triggers.

### 3. AI Monitoring Events
* `ai.provider.call.started`: Tracks prompt metadata, model settings, and target providers.
* `ai.provider.call.completed`: Emitted with billing metadata: token usage counts (input/output), pricing metrics, and generation latency.
* `ai.provider.call.failed`: Broadcasted if providers rate-limit, fail, or time out, enabling downstream fallbacks.

### 4. Plugin Events
* `plugin.lifecycle.loaded`: Notifies that a JAR has been validated and isolated inside its own Classloader.
* `plugin.lifecycle.failed`: Alerts when a plugin crashes or triggers security sandboxing rule violations.

---

## 5. Architectural Decision Record (ADR)

### ADR 0025: Coroutine-Native, Type-Safe Event Bus with Transactional Outbox

#### Status
Accepted

#### Date
2026-07-19

#### Context
A modern, decoupled automation system requires reacting to state changes asynchronously. Traditional message buses (like Spring ApplicationEventPublisher or Guava EventBus) either lack coroutine suspension integration, introduce rigid framework dependencies, or fail to propagate asynchronous tracing context. Furthermore, publishing events *before* database transactions commit results in data corruption if the database subsequently rolls back.

#### Decision
Design a lightweight, zero-dependency, coroutine-native `EventBus` that supports trace context propagation natively. The system will leverage Kotlin's `SharedFlow` and thread-safe channels for non-blocking publishing and subscription. Wildcard topics will be mapped using a Trie-based routing engine. Transactional consistency will be secured using the Outbox pattern.

#### Consequences
- Subsystems remain completely decoupled; they publish events onto the bus without knowing who consumes them.
- Subscriptions are managed as cancelable handles, preventing memory leaks when plugins load or unload dynamically.
- System tracing remains seamless across asynchronous thread dispatches, as trace headers are automatically carried over inside `EventEnvelope`.
- Distributed backends (Kafka/RabbitMQ) can be plugged in transparently using the `EventBridge` interface.
