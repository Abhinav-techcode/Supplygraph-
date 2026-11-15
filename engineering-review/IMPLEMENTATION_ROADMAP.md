# SupplyGraph — Phased Implementation Roadmap & Engineering Plan

**Document ID:** `SG-ENG-REV-009`  
**Author:** Principal Software Architect & Engineering Program Lead  
**Date:** November 2026  
**Status:** Implementation Roadmap Baseline  

---

## 1. Roadmap Overview & Execution Methodology

SupplyGraph will be implemented through an **incremental, gated engineering methodology**. 
- Each phase delivers a complete, testable vertical slice or foundational capability.
- **Strict Quality Gate:** No phase begins until the preceding phase meets 100% of its exit criteria, passes automated tests, and receives explicit stakeholder sign-off.
- **No monolithic code dumps:** Features are developed in small, reviewable commits with exhaustive unit and integration tests.

```
[Phase 0] ──► [Phase 1] ──► [Phase 2] ──► [Phase 3] ──► [Phase 4]
Baseline      Foundation    Domain & DT   Industry SPI  Disruptions
                                                        
      ┌───────────────────────────────────────────────────────┘
      ▼
[Phase 5] ──► [Phase 6] ──► [Phase 7] ──► [Phase 8] ──► [Phase 9]
Agentic AI    ConcurGuard   Opt & Sim     UI Decision   Hardening &
              & Replanning                Centre        Evaluation
```

---

## 2. Phase-by-Phase Execution Plan

### Phase 0 — Repository Baseline & Architecture Audit (Current Phase)
- **Objective:** Establish project understanding, verify technology compatibility, audit repository, and formulate architecture baseline.
- **Scope:** Documentation, technology matrix verification, ADR formulation. No application source code.
- **Deliverables:** 10 comprehensive review documents in `engineering-review/`.
- **Exit Criteria:** Review documents completed, technology compatibility verified, explicit user approval to proceed to Phase 1.

---

### Phase 1 — Build Foundation & Modular Monolith Skeleton
- **Objective:** Establish the Maven multi-module structure, Java toolchain, Spring Boot bootstrap, Vaadin shell, and Flyway baseline.
- **Scope:**
  - Root `pom.xml` with strict dependency management (Java 21 bytecode target, Spring Boot 3.4.1, Vaadin 24.5.4, Flyway 10.x).
  - Maven Wrapper (`./mvnw`, `.mvn/wrapper/`) for zero-dependency builds.
  - Core module directories: `core-domain`, `core-application`, `concurguard`, `simulation-engine`, `industry-api`, `adapter-persistence`, `adapter-ai`, `web`.
  - Spring Boot application entrypoint in `supplygraph-web`.
  - Minimal Vaadin Flow navigation shell (`MainLayout`, `HomeView`, `HealthView`).
  - PostgreSQL Testcontainers setup and baseline Flyway migration `V1__init_schema.sql`.
  - Health (`/actuator/health`) and readiness probes.
- **Tests:**
  - Context loads test (`SupplyGraphApplicationTests`).
  - Flyway migration test against Testcontainers PostgreSQL.
  - Vaadin view instantiation tests.
- **Exit Criteria:** `./mvnw clean verify` executes cleanly, application boots locally, database migrations execute on empty DB, health endpoint returns `UP`.

---

### Phase 2 — Core Domain & Digital Twin Graph Engine
- **Objective:** Implement the pure domain model, multi-tier supply network graph, inventory accounting, and state versioning.
- **Scope:**
  - Entities: `Workspace`, `Organization`, `Supplier`, `Facility` (Factory, Warehouse), `Item` (Material, Component, Product), `InventoryPosition`, `InventoryLot`, `CapacityResource`.
  - Edges: `SupplyEdge`, `BillOfMaterialsEdge` (DAG), `TransportRouteEdge`, `ProductionStageEdge`.
  - In-memory graph model with recursive dependency traversal.
  - State versioning engine (`stateVersion` tracking).
  - Unit of Measure (`Quantity`) and half-open time interval (`TimeWindow`) value objects.
  - Synthetic reference dataset generator (clearly labeled `[SYNTHETIC]`).
- **Tests:**
  - Domain unit tests for BOM acyclicity, non-negative inventory invariants, and unit conversions.
  - Multi-tier upstream/downstream graph traversal tests (Tier-3 to Tier-0).
  - JPA persistence integration tests with Testcontainers.
- **Exit Criteria:** Core domain passes 100% of invariant tests; graph traversal correctly discovers all affected nodes across 5 tiers in < 5ms.

---

### Phase 3 — Industry Extension Framework & EV Battery Pack
- **Objective:** Build the pluggable Industry SPI and deliver the first complete industry pack (EV Battery Manufacturing).
- **Scope:**
  - `supplygraph-industry-api`: `IndustryPackExtension` SPI, `IndustryConstraintPolicy`, `IndustryKpiDefinition`.
  - Dynamic `IndustryRegistry` for runtime discovery.
  - `supplygraph-industry-ev`:
    - Terminology: Cell chemistries (NMC, LFP), dry-rooms, formation aging, precursor grades.
    - 8 EV Disruption Scenarios (Lithium shortage, Nickel surge, shipment delay, temp excursion, equipment failure, quality rejection spike, demand surge, recycling disruption).
    - Hard constraints: Dry-room humidity limits, qualified cathode supplier policies.
- **Tests:**
  - SPI loading unit tests.
  - Scenario definition validation tests for all 8 EV scenarios.
  - EV domain constraint violation tests.
- **Exit Criteria:** EV Battery pack dynamically registers without modifying core modules; all 8 scenarios load with valid preconditions and constraint rules.

---

### Phase 4 — Disruption Management & Cascading Impact Analysis
- **Objective:** Implement the disruption incident lifecycle, operational event ingestion, and cascading downstream impact analysis.
- **Scope:**
  - `DisruptionIncident` aggregate lifecycle: `REPORTED` -> `INVESTIGATING` -> `RESOLVED`.
  - Event ingestion abstraction (`EventIngestionPort`, `OperationalEvent`).
  - Cascading impact analyzer: Time-to-impact ($T_{\text{impact}}$), inventory runway ($R_{\text{runway}}$), customer order exposure calculation.
  - Disruption simulation trigger for synthetic drills.
- **Tests:**
  - Incident state transition tests.
  - Impact calculation verification against multi-tier graph fixture.
  - Simultaneous disruption handling tests.
- **Exit Criteria:** System ingest synthetic events, traverses cascading impact paths, and calculates accurate financial exposure and runway hours.

---

### Phase 5 — Agentic AI Orchestration & Tool Execution Engine
- **Objective:** Implement the 8 specialized agents, Coordinator workflow, typed tool contracts, and the deterministic fake AI test harness.
- **Scope:**
  - Agent contracts and proposal record schemas (`AgentActionProposal`).
  - Typed, allow-listed Java tool interfaces (Read-only digital twin queries).
  - Server-side tool execution authorization and parameter validation.
  - Prompt injection sanitization and system/user delimiter boundaries.
  - `DeterministicFakeAiClient` returning repeatable proposal fixtures.
  - Spring AI adapter integration.
- **Tests:**
  - Unit tests verifying agent proposals conform strictly to JSON Schema.
  - Tool permission tests (unauthorized tool attempts rejected).
  - Malformed AI response, timeout, and retry tests.
- **Exit Criteria:** Coordinator invokes agents, agents retrieve data through allow-listed tools, and valid structured proposals are generated without mutating database state.

---

### Phase 6 — ConcurGuard Conflict Coordination & Selective Replanning
- **Objective:** Implement the 7-dimensional conflict detection engine, intent registry, arbitration state machine, and selective replanning algorithm.
- **Scope:**
  - `ActionIntent` and `ResourceClaim` normalization.
  - 7 conflict detectors: Quantity, Capacity, Temporal, Semantic, Dependency, Stale-State, Industry Policy.
  - Arbitration engine (`ALLOW`, `DELAY`, `RESCHEDULE`, `MODIFY`, `REJECT`, `REQUEST_REPLANNING`).
  - Selective Replanning engine: Dependency subgraph extraction, preservation of valid intents, minimal delta re-orchestration.
- **Tests:**
  - Comprehensive unit tests for every conflict class.
  - Adversarial tests with concurrent conflicting proposals.
  - Selective replanning tests verifying 100% preservation of unaffected actions.
- **Exit Criteria:** ConcurGuard reliably flags all 7 conflict types in deterministic test fixtures and repairs plans through minimal selective replanning.

---

### Phase 7 — Deterministic Optimization & Discrete-Event Simulation
- **Objective:** Integrate Google OR-Tools for mathematical optimization and implement the deterministic Java discrete-event simulation engine.
- **Scope:**
  - Google OR-Tools CP-SAT / Linear Solver integration for sourcing and capacity allocation under hard constraints.
  - Deterministic simulation engine: Discrete-event calendar, virtual clock, reproducible random seeds.
  - Comparative simulation branches: Baseline, Disrupted, Recovery Plan.
  - Multi-objective scoring: Recovery Cost, Order Delays, Fill Rate.
- **Tests:**
  - Optimization solver feasibility tests with known optimal solutions.
  - Simulation determinism tests (identical inputs yield bit-identical outputs).
  - Performance tests with 10,000 simulated events.
- **Exit Criteria:** Optimization engine finds optimal allocations within 2-second timeout; simulation reproduces exact timeline outcomes across repeated runs.

---

### Phase 8 — Enterprise Decision Centre UI & Remaining Industry Packs
- **Objective:** Build the complete Vaadin Flow decision centre and implement the remaining 3 industry packs (Pharma, Semiconductor, Automotive).
- **Scope:**
  - Vaadin Views:
    - Digital Twin Graph View (Interactive network visualization).
    - Active Disruptions & Cascading Impact Tree.
    - Agent Investigation & Proposal Review.
    - ConcurGuard Conflict Matrix & Selective Replanning Inspector.
    - Simulation Comparison Dashboard (Baseline vs. Disrupted vs. Recovered).
    - Human Approval & Governance Audit Log.
  - Industry Packs:
    - `supplygraph-industry-pharma` (8 scenarios, cold-chain, GMP).
    - `supplygraph-industry-semiconductor` (8 scenarios, wafer yields, cleanrooms).
    - `supplygraph-industry-automotive` (8 scenarios, JIT/JIS, stamping).
- **Tests:**
  - Vaadin component unit tests and end-to-end user journey tests.
  - Acceptance tests for all 32 industry scenarios.
- **Exit Criteria:** All 4 industry packs operational with 32 verified scenarios; human operators can review evidence and execute approvals from the UI.

---

### Phase 9 — Security Hardening, Empirical Evaluation & Final Release
- **Objective:** Complete security audit, RBAC enforcement, empirical research benchmarking, and operational readiness documentation.
- **Scope:**
  - Spring Security role-based access control (Admin, Analyst, Planner, Approver).
  - Insecure Direct Object Reference (IDOR) audit across all endpoints.
  - Empirical evaluation experiments: Benchmarking ConcurGuard against uncoordinated and locking baselines.
  - Reproducibility report and experimental methodology document.
  - Docker Compose packaging and production deployment guide.
- **Tests:**
  - Security penetration & authorization test suite.
  - Full regression test suite across all 32 scenarios.
  - Performance and load benchmarking.
- **Exit Criteria:** Zero high-severity security vulnerabilities; empirical benchmark results documented; complete documentation signed off.

