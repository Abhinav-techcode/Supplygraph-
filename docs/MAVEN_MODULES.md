# SupplyGraph — Maven Module Responsibility Matrix

**Document ID:** `SG-DOC-003`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Official Module Catalog (Updated for Phases 1, 2 & 3)  

---

## 1. Complete Module Catalog

| Module Artifact ID | Packaging | Layer | Architectural Responsibility |
|---|---|---|---|
| `supplygraph` (root) | `pom` | Root | Parent POM, BOM dependency management, compiler configuration. |
| `supplygraph-core-domain` | `jar` | Domain | Pure Java 21 aggregates, entities, value objects, domain invariants, repository ports. |
| `supplygraph-industry-api` | `jar` | Domain | Service Provider Interface (SPI) contracts for industry packs and scenarios. |
| `supplygraph-concurguard` | `jar` | Domain / Engine | Semantic plan conflict detection engine, intent registry, selective replanning. |
| `supplygraph-simulation-engine`| `jar` | Domain / Engine | Discrete-event simulation clock and Google OR-Tools optimization integration. |
| `supplygraph-core-application`| `jar` | Application | Application use cases, command/query handlers, transactional boundaries. |
| `supplygraph-industry-ev` | `jar` | Industry Pack | EV Battery intelligence pack, precursor constraints, 8 disruption scenarios. |
| `supplygraph-industry-pharma` | `jar` | Industry Pack | Pharmaceutical pack, cold-chain excursion rules, 8 disruption scenarios. |
| `supplygraph-industry-semiconductor` | `jar` | Industry Pack | Semiconductor pack, wafer yield models, cleanroom bottlenecks, 8 scenarios. |
| `supplygraph-industry-automotive` | `jar` | Industry Pack | Automotive pack, JIT/JIS delivery rules, stamping bottlenecks, 8 scenarios. |
| `supplygraph-adapter-persistence`| `jar` | Infrastructure | PostgreSQL 16+ relational adapter, Spring Data JPA repositories, Flyway migrations. |
| `supplygraph-adapter-ai` | `jar` | Infrastructure | Spring AI model adapters, typed tools, deterministic fake AI test harness. |
| `supplygraph-web` | `jar` | Presentation | Composition root, Spring Boot entrypoint, Vaadin Flow Java UI views. |

---

## 2. Inward Dependency Graph

```
[supplygraph-web]
       │
       ├───────────────────────────────────┐
       ▼                                   ▼
[supplygraph-adapter-persistence]   [supplygraph-adapter-ai]
       │                                   │
       ├───────────────────────────────────┘
       ▼
[supplygraph-core-application]
       │
       ├─────────────────┬─────────────────┐
       ▼                 ▼                 ▼
[supplygraph-concurguard] [supplygraph-simulation-engine] [supplygraph-industry-*]
       │                                   │                      │
       ├───────────────────────────────────┴──────────────────────┘
       ▼
[supplygraph-industry-api]
       │
       ▼
[supplygraph-core-domain]  ◄── PURE JAVA 21 (ZERO EXTERNAL DEPENDENCIES)
```



---

## 4. Phase 2 Package Structure Additions

### `supplygraph-core-domain` (Pure Java 21)
- `com.supplygraph.domain.model.common`: `UnitOfMeasure`, `Quantity`
- `com.supplygraph.domain.model.graph`: `NodeId`, `EdgeId`, `NodeType`, `TransportMode`, `SupplyChainNode`, `Supplier`, `ManufacturingSite`, `Warehouse`, `LogisticsHub`, `TransportationLane`, `SupplyChainGraph`
- `com.supplygraph.domain.model.bom`: `MaterialId`, `MaterialType`, `Material`, `Component`, `FinishedProduct`, `BomEntry`, `BillOfMaterials`
- `com.supplygraph.domain.model.inventory`: `LotNumber`, `ExpirationDate`, `InventoryLot`, `BalanceCategory`, `InventoryAccountId`, `InventoryAccount`, `InventoryBalance`, `PostingType`, `InventoryPosting`, `TransactionId`, `InventoryTransaction`, `ReservationId`, `Reservation`, `InventoryLedger`
- `com.supplygraph.domain.model.capacity`: `ResourceId`, `CapacityReservationId`, `CapacityWindow`, `OperatingSchedule`, `CapacityReservation`, `ResourceCapacity`, `CapacityCalendar`, `Resource`
- `com.supplygraph.domain.model.twin`: `StateVersion`, `DomainEvent`, `StateTransition`, `DigitalTwinSnapshot`, `DigitalTwinState`

### `supplygraph-core-application`
- `com.supplygraph.application.port.in`: `DigitalTwinUseCase`
- `com.supplygraph.application.port.out`: `DigitalTwinRepositoryPort`, `InventoryLedgerRepositoryPort`
- `com.supplygraph.application.service`: `DigitalTwinApplicationService`

### `supplygraph-adapter-persistence`
- `com.supplygraph.adapter.persistence.entity`: `WorkspaceJpaEntity`, `DigitalTwinStateJpaEntity`, `InventoryBalanceJpaEntity`
- `com.supplygraph.adapter.persistence.repository`: `SpringDataWorkspaceRepository`, `SpringDataDigitalTwinStateRepository`, `SpringDataInventoryBalanceRepository`
- `com.supplygraph.adapter.persistence.adapter`: `PostgresDigitalTwinRepositoryAdapter`, `PostgresInventoryLedgerRepositoryAdapter`
- `db.migration`: `V1__init_schema.sql`, `V2__digital_twin_schema.sql`

---

## 5. Phase 3 Package Structure Additions

### `supplygraph-concurguard` (Domain Engine)
- `com.supplygraph.concurguard.model`: `ActionIntent`, `IntentId`, `AgentId`, `ActionType`, `ClaimType`, `ResourceClaim`, `CapacityClaim`, `ResourceIdentity`, `ResourceAliasRegistry`
- `com.supplygraph.concurguard.conflict`: `ConcurGuardConflictDetector`, `QuantityConflictDetector`, `CapacityConflictDetector`, `TemporalConflictDetector`, `SemanticAliasConflictDetector`, `DependencyConflictDetector`, `StaleStateConflictDetector`, `IndustryPolicyConflictDetector`, `Conflict`, `ConflictId`, `Severity`
- `com.supplygraph.concurguard.arbitration`: `ConcurGuardArbitrator`, `ArbitrationDecision`, `DecisionId`
- `com.supplygraph.concurguard.replanning`: `IntentDependencyGraph`, `ReplanningScope`, `ReplanningResult`, `SelectiveReplanningCoordinator`
- `com.supplygraph.concurguard.benchmark`: `StrategyType`, `BenchmarkMetrics`, `ConcurGuardBenchmarkRunner`
- `com.supplygraph.concurguard.api`: `ConcurGuardEngine`, `DefaultConcurGuardEngine`, `ConflictType`, `ArbitrationStatus`

### `supplygraph-core-application`
- `com.supplygraph.application.agent`: `AgentRole`, `AgentIdentity`, `DisruptionIncident`, `AgentInvestigationContext`, `AgentProposal`, `SpecializedAgent`
- `com.supplygraph.application.tool`: `AgentTool`, `ToolExecutionRequest`, `ToolExecutionResult`, `ToolExecutionBoundary`
- `com.supplygraph.application.tool.builtin`: `QueryInventoryTool`, `QueryBomTool`, `QueryLaneStatusTool`, `QueryCapacityTool`
- `com.supplygraph.application.recovery`: `RecoveryProposal`, `AgenticRecoveryOrchestrator`

### `supplygraph-adapter-ai`
- `com.supplygraph.adapter.ai`: `DeterministicFakeAiClient`
- `com.supplygraph.adapter.ai.agent`: `BaseSpecializedAgent`, `CoordinatorAgent`, `RiskIntelligenceAgent`, `InventoryAgent`, `ProcurementAgent`, `LogisticsAgent`, `ProductionAgent`, `QualityAgent`, `RecoveryPlanningAgent`
- `com.supplygraph.adapter.ai.exception`: `AiTimeoutException`, `AiProviderException`, `AiMalformedResponseException`
