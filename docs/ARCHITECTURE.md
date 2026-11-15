# SupplyGraph — Architecture & System Design Document

**Document ID:** `SG-DOC-002`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Official Architectural Specification (Phases 1, 2 & 3 Completed)  

---

## 1. Architectural Style: Modular Monolith

SupplyGraph is architected as an enterprise-grade **Maven multi-module modular monolith**.
- Eliminates distributed network failures, serialization latencies, and 2-phase commit (2PC) consensus overhead during multi-agent plan synthesis and ConcurGuard arbitration.
- Strict compile-time and test-time architectural boundaries are enforced using Maven module boundaries and ArchUnit architecture tests.

```
                    +────────────────────────────────────+
                    |       PRESENTATION LAYER           |
                    |     Vaadin Flow Views & Shell      |
                    +─────────────────┬──────────────────+
                                      │ depends on
                                      ▼
                    +────────────────────────────────────+
                    |        APPLICATION LAYER           |
                    |  Use Cases, Commands, Orchestrators|
                    +─────────────────┬──────────────────+
                                      │ depends on
                                      ▼
                    +────────────────────────────────────+
                    |           DOMAIN LAYER             |
                    | Entities, Invariants, Domain Logic |
                    +─────────────────┬──────────────────+
                                      ▲
                                      │ implements ports (DIP)
                    +─────────────────┴──────────────────+
                    |       INFRASTRUCTURE LAYER         |
                    | JPA/Postgres, Spring AI, OR-Tools  |
                    +────────────────────────────────────+
```

---

## 2. Inward Dependency Rule & Hexagonal Principles

Dependencies flow strictly inward toward the pure domain layer:

1. **Domain Layer (`supplygraph-core-domain`):**
   - 100% pure Java 21 standard library.
   - Zero dependencies on Spring, JPA, Hibernate, Vaadin, Jackson, or AI SDKs.
   - Houses aggregates, entities, value objects, domain invariants, and repository ports.
2. **Application Layer (`supplygraph-core-application`):**
   - Implements use case orchestration, transactional boundaries (`@Transactional`), and application events.
3. **Infrastructure Adapters:**
   - Persistence (`supplygraph-adapter-persistence`): PostgreSQL 16+, Spring Data JPA, and Flyway.
   - AI (`supplygraph-adapter-ai`): Spring AI ChatClient, typed tools, and deterministic fake AI harness.
   - Optimization & Simulation (`supplygraph-simulation-engine`): Google OR-Tools JNI and discrete-event clock.
   - Coordination (`supplygraph-concurguard`): Intent registry, 7-D conflict detector, and selective replanner.
4. **Presentation (`supplygraph-web`):**
   - 100% Java-authored Vaadin Flow UI.
   - Eliminates JavaScript/TypeScript context switching and duplicate DTO maintenance.

---

## 3. Pluggable Industry Pack Extension SPI

The shared platform core is closed to modification but open to extension (Open/Closed Principle):
- `supplygraph-industry-api` defines the `IndustryPackExtension` SPI.
- Industry modules (`supplygraph-industry-ev`, `pharma`, `semiconductor`, `automotive`) implement the SPI and register dynamically at application startup.
- Adding future industry packs requires zero modifications to shared domain or application logic.



---

## 4. Phase 2 — Digital Twin Core Architecture

### 4.1 Canonical Multi-tier Supply Chain Graph
The network topology is modeled as a directed multi-tier graph in `com.supplygraph.domain.model.graph`:
- **Nodes (`SupplyChainNode`):** Specialized into `Supplier` (tier, risk score, certifications), `ManufacturingSite` (production lines, cleanroom class, throughput), `Warehouse` (storage area, cold-chain, HAZMAT), and `LogisticsHub` (cross-docking, daily throughput).
- **Edges (`TransportationLane`):** Directed connections capturing transport mode (AIR, OCEAN, ROAD, RAIL, INTERMODAL), transit duration, and unit freight cost. Self-loops and dangling endpoints are strictly rejected.
- **Graph Traversal (`SupplyChainGraph`):** Provides deterministic DFS-based upstream supplier discovery, downstream impact reachability, and exhaustive multi-path route enumeration.

### 4.2 Multi-tier Bill of Materials (BOM) DAG
Material dependency hierarchies in `com.supplygraph.domain.model.bom` enforce:
- **Strict Cycle Detection:** 3-color DFS marking guarantees directed acyclic graph (DAG) topology on construction; cycles trigger `IllegalStateException`.
- **Topological Sorting:** Deterministic Kahn's algorithm yields bottom-up dependency ordering from raw materials to finished products.
- **Yield/Scrap Propagation:** Multi-level requirement explosion calculates exact scrap-adjusted gross requirements:
  $$\text{Gross} = \text{Net} \times (1 + \text{scrapRate})$$
- **High-Precision Decimal Arithmetic:** Authoritative quantities use `BigDecimal` with scale 4 and `RoundingMode.HALF_UP` (zero floating-point arithmetic).

### 4.3 Double-Entry Inventory Ledger
Inventory accounting in `com.supplygraph.domain.model.inventory` models inventory balances with mathematical conservation:
- **Categories:** `ON_HAND_AVAILABLE`, `RESERVED_PRODUCTION`, `RESERVED_SHIPMENT`, `IN_TRANSIT`, `QUARANTINED`, `EXTERNAL_OFFSET`.
- **Conservation Guarantee:** Every transaction is balanced ($\sum \text{Debits} = \sum \text{Credits}$).
- **Non-Negative Invariant:** Internal asset accounts cannot drop below zero.
- **Lot Traceability & Expiration:** Lots track batch numbers and expiration dates; expired lots are blocked from `ON_HAND_AVAILABLE`.
- **Explicit Reservation Lifecycles:** Allocates inventory atomically from available to reserved, and releases or fulfills via balanced ledger postings.

### 4.4 Resource Capacity & Half-Open Intervals
Resource processing throughput in `com.supplygraph.domain.model.capacity` models:
- **Half-Open Intervals $[start, end)$:** Prevents boundary double-counting.
- **Capacity Calendars:** Real-time overlap detection and remaining capacity availability calculation over query horizons.

### 4.5 Digital Twin State Machine & Version Progression
- **Aggregate Root (`DigitalTwinState`):** Strictly monotonic state version progression ($v_0 \to v_1 \to v_2$).
- **Event Deduplication:** Domain event IDs are deduplicated to ensure idempotent state updates.
- **Immutable Snapshots (`DigitalTwinSnapshot`):** Point-in-time state captures isolate simulations from production state.

---

## 5. Phase 3 — Agentic AI Orchestration & ConcurGuard Engine

### 5.1 Bounded Specialized Agents
Eight domain-bounded agents (`Coordinator`, `RiskIntelligence`, `Inventory`, `Procurement`, `Logistics`, `Production`, `Quality`, `RecoveryPlanning`) operate under proposal-only constraints:
- Pure read-only inspection through `DigitalTwinSnapshot`.
- Zero mutating authority over live production state.
- Structured proposals with verifiable evidence, explicit assumptions, and quantified confidence.

### 5.2 Zero-Trust Tool Execution Boundary
Centralized Java execution gateway (`ToolExecutionBoundary`) enforcing:
- Workspace tenant isolation.
- Agent identity and role-based tool allow-listing.
- Invocations rate limits (preventing recursive loops).
- Immutable audit logging for every tool call attempt.

### 5.3 ConcurGuard Deterministic Coordination & Replanning
Mathematical conflict detection and arbitration eliminating LLM hallucination:
- **7 Conflict Classes:** Quantity scarcity, capacity overcommit, temporal sequencing, semantic aliases, dependency deadlocks, stale state, and regulatory policies.
- **Strict Precedence Arbitration:** Safety/regulatory holds > Deadlock cycles > Stale state > Quantitative scarcity.
- **Selective Replanning:** Minimal subgraph invalidation preserves clean, independent proposals while targeting only affected agents.
- **Proposal Mode Exclusivity:** Recovery proposals are persisted for human decision-maker approval with complete cost, RTO, and audit metrics.
