# 04 — Target Architecture

## Architectural style

Start with a **Maven multi-module modular monolith**. Keep domain boundaries explicit and modules independently testable. Do not start with distributed microservices. Extract a service only when measured scale, deployment independence, ownership or reliability requirements justify it.

## Logical layers

1. **Presentation:** Vaadin Flow views, components and navigation.
2. **Application:** use cases, orchestration, transaction boundaries and authorization.
3. **Domain:** entities, value objects, policies, invariants and domain services.
4. **Infrastructure:** persistence adapters, graph access, messaging, LLM provider adapters and external integrations.

Dependencies should point inward. Domain logic must not depend on Vaadin, Spring MVC, Kafka clients or LLM SDKs.

## Proposed Maven modules

- `supplygraph-domain` — shared domain primitives and core contracts; keep it small.
- `supplygraph-application` — use cases and orchestration interfaces.
- `supplygraph-industry-api` — industry extension contracts.
- `supplygraph-industry-ev`
- `supplygraph-industry-pharma`
- `supplygraph-industry-semiconductor`
- `supplygraph-industry-automotive`
- `supplygraph-adapter-persistence` — PostgreSQL/JPA adapters.
- `supplygraph-adapter-graph` — Neo4j adapter, if graph database is justified.
- `supplygraph-adapter-messaging` — Kafka adapter, if asynchronous event volume/workflows justify it.
- `supplygraph-adapter-ai` — Spring AI provider and tool adapters.
- `supplygraph-web` — Vaadin application and composition root.
- `supplygraph-bootstrap` — optional executable packaging module; avoid duplication if `web` is the executable.

Review this module list before creating it. Do not create empty modules merely to match a diagram. A simpler initial module set is acceptable if boundaries remain clean.

## Core flow

1. A disruption is recorded with provenance and validated.
2. Impact analysis queries the digital twin at a known state version.
3. Coordinator creates bounded investigation tasks.
4. Agents return structured evidence and action intents.
5. ConcurGuard normalizes intents and checks semantic, resource, temporal and dependency conflicts.
6. Optimizer evaluates feasible alternatives.
7. Simulator evaluates candidate plans against a versioned state.
8. Human reviews recommendation, evidence and trade-offs.
9. Approval creates a controlled state transition; no external side effect occurs without explicit configured authorization.
10. Monitoring revalidates the plan as new events arrive.

## Data ownership

- PostgreSQL is the authoritative transactional store for users, workspaces, incidents, inventory, orders, intents, approvals, audit and experiment records.
- Neo4j, if retained, is a graph-query projection or a clearly governed graph store. Define synchronization and consistency semantics; do not create two competing sources of truth.
- Kafka, if retained, transports events. It is not the source of truth.
- LLM provider responses are untrusted external input and must be schema-validated.

## Architecture decisions to record (ADRs)

Create an ADR for:
- Modular monolith and module boundaries.
- Vaadin Flow and Java-authored UI.
- PostgreSQL ownership and graph projection strategy.
- Whether Neo4j is needed for the initial scale.
- Whether Kafka is needed in the first release or an in-process event abstraction is sufficient initially.
- Spring AI provider abstraction and structured output strategy.
- State versioning and intent reservation semantics.
- Simulation time model and determinism.

## Technology boundary

The product application is Java-first. Do not create a React/Node frontend or Python application service. Browser-side internals used by Vaadin are not a separately maintained project frontend. External database, broker and LLM runtimes are infrastructure dependencies, not Java application modules.
