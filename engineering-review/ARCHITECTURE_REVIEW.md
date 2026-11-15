# SupplyGraph — Architecture Review & System Design

**Document ID:** `SG-ENG-REV-003`  
**Author:** Principal Software Architect & Enterprise Systems Designer  
**Date:** November 2026  
**Status:** Architecture Review Baseline  

---

## 1. Architectural Style: Modular Monolith vs. Microservices

### 1.1 The Decision: Modular Monolith
SupplyGraph adopts a **Maven multi-module modular monolith** as its foundational architecture. Distributed microservices are explicitly rejected for the initial baseline and foreseeable development milestones.

### 1.2 Rationale & Trade-off Analysis
```
+--------------------------------------------------------------------------------------------------+
|                                MODULAR MONOLITH vs. MICROSERVICES                               |
+----------------------------------+----------------------------------+----------------------------+
| Architectural Dimension          | Modular Monolith (SupplyGraph)   | Distributed Microservices  |
+----------------------------------+----------------------------------+----------------------------+
| Transactional Integrity          | ACID across relational entities; | 2-Phase Commit / Sagas;    |
|                                  | in-process consistency           | eventual consistency risks |
| Network Latency & Overhead       | Zero IPC overhead; in-memory     | HTTP/gRPC serialization;   |
|                                  | method calls and event dispatch  | network jitter and retries |
| ConcurGuard Synchronization      | Ultra-fast memory intent table;  | Distributed locking / Raft |
|                                  | atomic plan conflict checks      | consensus overhead         |
| Operational Complexity           | Single deployable artifact;      | Kubernetes, service mesh,  |
|                                  | standard PostgreSQL database     | distributed tracing, etc.  |
| Developer Velocity & Refactoring | Type-safe compile-time checks    | Breaking API changes,      |
|                                  | across module boundaries         | multi-repo synchrony       |
+----------------------------------+----------------------------------+----------------------------+
```

A modular monolith enforces the exact same strict logical boundaries and domain encapsulation as microservices through **module visibility, internal packaging, and interface contracts**, without the devastating latency and operational penalties of distributed systems.

---

## 2. Logical Layers & Hexagonal Principles

The system strictly follows **Clean Architecture / Ports & Adapters (Hexagonal Architecture)**. Dependencies point strictly inward toward the core domain.

```
                    +------------------------------------+
                    |       PRESENTATION LAYER           |
                    |     Vaadin Flow Views & Shell      |
                    +-----------------+------------------+
                                      │ depends on
                                      ▼
                    +------------------------------------+
                    |        APPLICATION LAYER           |
                    |  Use Cases, Commands, Orchestrators|
                    +-----------------+------------------+
                                      │ depends on
                                      ▼
                    +------------------------------------+
                    |           DOMAIN LAYER             |
                    | Entities, Invariants, Domain Logic |
                    +-----------------+------------------+
                                      ▲
                                      │ implements ports (DIP)
                    +-----------------+------------------+
                    |       INFRASTRUCTURE LAYER         |
                    | JPA/Postgres, Spring AI, OR-Tools  |
                    +------------------------------------+
```

### 2.1 The Four Logical Layers
1. **Domain Layer (Core):**
   - Pure Java (zero Spring, zero persistence, zero web dependencies).
   - Houses entities (`Facility`, `InventoryPosition`), value objects (`Quantity`, `TimeWindow`), domain invariants, repository interfaces (ports), and domain events.
2. **Application Layer:**
   - Implements use cases (e.g., `RegisterDisruptionUseCase`, `ExecuteSelectiveReplanningUseCase`).
   - Manages transactional boundaries (`@Transactional`), security authorization, and application event publishing.
3. **Infrastructure Layer (Adapters):**
   - Implements domain repository interfaces using Spring Data JPA / Hibernate / PostgreSQL.
   - Contains the Spring AI client adapters and deterministic fake AI adapters.
   - Implements the OR-Tools optimization solvers and discrete-event simulation drivers.
4. **Presentation Layer (Vaadin Flow):**
   - Authored entirely in Java using Vaadin Flow components (`VerticalLayout`, `Grid`, `Dialog`).
   - Controllers and view classes remain thin; they delegate immediately to application use cases.

---

## 3. Industry Extension Architecture (SPI)

A critical requirement is that **adding an industry pack must never require modifying shared core code** (Open/Closed Principle).

### 3.1 The Extension SPI (`supplygraph-industry-api`)
The shared core defines an explicit Service Provider Interface (SPI):

```java
public interface IndustryPackExtension {
    IndustryIdentifier getId();
    String getDisplayName();
    SemanticVersion getVersion();
    
    // Domain metadata & constraints
    IndustryTerminology getTerminology();
    List<IndustryConstraintPolicy> getConstraintPolicies();
    List<IndustryKpiDefinition> getKpiDefinitions();
    
    // Scenario definitions & tool capabilities
    List<DisruptionScenarioDefinition> getScenarioDefinitions();
    Set<AgentToolCapability> getAllowedAgentCapabilities();
    
    // Data mapping & validation
    DataIngestionMapper getDataIngestionMapper();
    List<ScenarioAcceptanceTestFixture> getAcceptanceTestFixtures();
}
```

### 3.2 Dynamic Registration & Inversion of Control
- Each industry module (`supplygraph-industry-ev`, `supplygraph-industry-pharma`, etc.) implements `IndustryPackExtension` and registers as a Spring bean (or Java `ServiceLoader`).
- The `IndustryRegistry` in the application core discovers all available packs at startup.
- Core business workflows evaluate policies polymorphically:
  ```java
  // Clean polymorphic dispatch — NEVER: if (industry == "PHARMA")
  industryRegistry.get(workspace.getIndustry())
      .getConstraintPolicies()
      .forEach(policy -> policy.validate(actionIntent, digitalTwinContext));
  ```

---

## 4. Recommended Maven Multi-Module Structure

```
supplygraph/ (root POM)
├── pom.xml                                [Parent BOM & Dependency Management]
├── supplygraph-core-domain/               [Pure Domain Entities, Value Objects, Ports]
├── supplygraph-core-application/          [Use Cases, Orchestrators, Application Services]
├── supplygraph-concurguard/               [Conflict Engine, Intent Registry, Replanner]
├── supplygraph-simulation-engine/         [Discrete-Event Simulator, OR-Tools Optimizer]
├── supplygraph-industry-api/              [SPI Contracts for Industry Extensions]
├── supplygraph-industry-ev/               [EV Battery Domain Rules & 8 Scenarios]
├── supplygraph-industry-pharma/           [Pharma GMP Rules, Cold-Chain & 8 Scenarios]
├── supplygraph-industry-semiconductor/    [Semiconductor Fab/Yield Rules & 8 Scenarios]
├── supplygraph-industry-automotive/       [Automotive JIT/JIS Rules & 8 Scenarios]
├── supplygraph-adapter-persistence/       [PostgreSQL, Flyway Migrations, JPA Repos]
├── supplygraph-adapter-ai/                [Spring AI Client, Typed Tools, Fake Provider]
└── supplygraph-web/                       [Vaadin Flow UI Views, Application Bootstrap]
```

### 4.1 Module Responsibility & Dependency Rules

| Module Name | Allowed Dependencies | Prohibited Dependencies |
|---|---|---|
| `supplygraph-core-domain` | *None* (pure Java standard library) | Spring, JPA, Vaadin, Jackson, AI SDKs |
| `supplygraph-industry-api` | `supplygraph-core-domain` | Persistence, UI, Spring Web |
| `supplygraph-concurguard` | `supplygraph-core-domain`, `supplygraph-industry-api` | Spring Web, Vaadin, JPA Entities |
| `supplygraph-simulation-engine`| `supplygraph-core-domain`, OR-Tools Java | Spring Web, Vaadin |
| `supplygraph-core-application`| `core-domain`, `industry-api`, `concurguard`, `simulation-engine` | Vaadin, Servlet API |
| `supplygraph-industry-*` | `core-domain`, `industry-api` | UI, Persistence, other industry packs |
| `supplygraph-adapter-persistence`| `core-domain`, Spring Data JPA, Flyway, PostgreSQL | Vaadin, AI SDKs |
| `supplygraph-adapter-ai` | `core-domain`, `core-application`, Spring AI | Vaadin, Persistence |
| `supplygraph-web` | *All modules* (Composition Root) | Direct circular references |

---

## 5. Architectural Decision Records (ADRs) Summary

- **ADR-001: Modular Monolith vs. Microservices**  
  *Decision:* Implement a Maven multi-module modular monolith. Microservice boundaries are rejected due to transactional coordination overhead during multi-action plan synthesis.
- **ADR-002: PostgreSQL as Relational Truth & Graph Traversal**  
  *Decision:* PostgreSQL 16+ is the primary source of truth. Supply network graph relationships and cascading BOM traversals are executed using PostgreSQL Recursive Common Table Expressions (CTEs) and in-memory graph models. Neo4j is deferred until dataset scale (>1M nodes) justifies distributed dual-write synchronization.
- **ADR-003: In-Process Event Coordination vs. Kafka**  
  *Decision:* Initial release uses Spring ApplicationEventPublisher / in-memory event channels. Kafka is deferred because single-instance transactional event distribution eliminates message broker operational overhead and network failure modes.
- **ADR-004: Vaadin Flow for Enterprise UI**  
  *Decision:* Vaadin Flow is chosen to provide a 100% Java-authored frontend. This eliminates JavaScript/TypeScript context switching, prevents frontend-backend DTO duplication, and ensures type-safe interaction with domain models.
- **ADR-005: Strict Boundary for Agentic AI**  
  *Decision:* Agents propose action intents via structured JSON/Java records; agents never execute SQL or mutate database state directly. Authoritative calculations (inventory balance, capacity, costs) are strictly reserved for deterministic Java services.

