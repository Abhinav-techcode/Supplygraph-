# SupplyGraph — Intended versus Actual

Evidence date: 2026-11-18. Inspected tree: `/Users/abhinavgupta/Desktop/Supplygraph` (branch `phase-9-10-integration-runtime-and-governed-execution`, HEAD `9dfc00c`, plus ~230 uncommitted files that include the Phase 11–15 work). Browser evidence comes from the running instance on port 8080 (local H2 profile). Nothing was modified to produce this document.

Status scale: **FA** fully aligned and verified · **IN** implemented but not fully aligned · **PI** partially implemented · **PO** presentation only / placeholder · **NI** not implemented · **BU** blocked or unverifiable.

Rule applied: a class, route or test existing is *not* evidence of alignment. "Verified" means I saw it work on real persisted domain data end to end.

## Summary table

| # | Intended capability | Status | Backend logic | Uses persisted domain data | Uses the real graph | Genuine AI | Operable in UI | End-to-end |
|---|---|---|---|---|---|---|---|---|
| 1 | Living digital twin | **PI** | yes (versioned state, ledger, snapshots) | **no** – only `(workspace, version)` is persisted | n/a | n/a | read-only | no |
| 2 | Multi-tier supplier/material dependency intelligence | **IN** | yes | in-memory twin | yes | no | yes | partial |
| 3 | Accurate interactive supply-chain graph | **PO** (UI) / IN (domain) | domain graph yes | **no – UI uses hard-coded nodes** | **no** | n/a | click/filter only | no |
| 4 | Disruption detection and propagation | **IN** | rules engine + topological propagation | events persisted; twin in-memory | yes (reachability) | no | yes | partial |
| 5 | Specialist AI agents | **PO** | fixture proposals | none | none | **none** | **not invoked from UI** | no |
| 6 | ConcurGuard | **IN** | 9 real detectors | snapshot of in-memory twin | no | n/a | via Decision Centre | thinly fed |
| 7 | Discrete-event + Monte Carlo simulation | **IN / BU** | real engines | caller-supplied parameters | not verified | no | forms | not verified end to end |
| 8 | OR-Tools optimization | **IN** | real GLOP/SCIP LP | **candidates hard-coded in the view** | no | no | yes | partial |
| 9 | Multi-echelon planning | **PI** | forecasting/reorder algorithms | own tables, **no twin/BOM/ledger link** | no | no | yes | no |
| 10 | Explainable recommendations | **PI** | template explanations | plans are 3 canned templates | no | no | yes | partial |
| 11 | Human-approved governed execution | **IN** | strong (state machines, SoD, evidence) | yes (PostgreSQL) | no | no | yes | verified, **simulated only** |
| 12 | Outcome reconciliation | **PI** | compare + propose | yes | no | no | yes | **twin not updated; no learning** |
| 13 | Supplier collaboration | **PI** | portal, RFQ, docs | yes | no | no | yes | **no effect on operations** |
| 14 | Enterprise integration | **IN** | outbox, webhooks, REST adapter | yes | no | n/a | yes | verified locally only |
| 15 | Sustainability / carbon / traceability | **PI** | calculators, genealogy | own tables | **no link to materials/lots** | no | yes | island |
| 16 | Dashboards, role experiences | **PO** (partly) | – | **KPIs are literals** | – | – | yes | no |
| 17 | Industry configuration/extensibility | **PI** | 4 one-class packs | not consumed by services | – | – | view only | no |

Count: **FA 0 of 17.** IN 7, PI 7, PO 3 (graph UI, agents, dashboards).

## Evidence by capability

### 1. Living digital twin — PI
* `DigitalTwinState` (core-domain) is a rich in-memory aggregate: versioned transitions, inventory ledger, capacity reservations, immutable snapshots (`DigitalTwinStateTest`).
* `PostgresDigitalTwinRepositoryAdapter.save` stores the state in a `ConcurrentHashMap` and persists **only** `id, version, updated_at` (`DigitalTwinStateJpaEntity`). `findByWorkspaceId` after a restart returns `new DigitalTwinState(ws, version, null, null)` — an **empty graph and BOM**.
* V2 migration creates `supply_chain_nodes`, `transportation_lanes`, `bom_entries`, `inventory_*` tables, but **no JPA entity or adapter maps them** (`grep` finds none). They are dead schema.
* The twin is created at every start by `DemoDataInitializer` (hard-coded 17 nodes). There is no import, edit, or "load a supply chain" path (the Phase 7 import service injects the twin repository but never uses it).
* `DigitalTwinApplicationService.postInventoryTransaction/bookCapacityReservation` have no callers outside tests.

### 3. Graph — PO for the visualization
* `DigitalTwinNetworkGraphComponent` has a no-argument constructor and builds `VisualNode`/`VisualLane` from literals (`initializeTopologyData`, 17 nodes, ~18 lanes). Disrupted flags (`FAB-MUNICH`, `PORT-ROTTERDAM`, lane L-5) are constants.
* `DigitalTwinGraphExplorerView` repeats the same data a second time as `sampleNodes`/`sampleLanes` tables (30 literals).
* The Home page and the explorer describe the graph as "LIVE", "real-time capacity, inventory" — it is neither.
* Conclusion in `GRAPH_ARCHITECTURE_ASSESSMENT.md`.

### 5. Specialist AI agents — PO
* The eight agents (`supplygraph-adapter-ai/.../agent/*Agent.java`) return **fixed `AgentProposal` literals**: constant confidence (e.g. Logistics 0.91/0.94), constant risk scores, constant evidence strings ("Carrier dispatch confirmed"). The result of the tool call is discarded.
* `DeterministicFakeAiClient` is a test double; no model client exists in `supplygraph-web`'s configuration; Spring AI is only a dependency of the adapter module.
* `AgenticRecoveryOrchestrator` is not referenced by any view or service. `AgentInvestigationHistoryView` renders **four hard-coded rows** (`INV-001…004`).
* Real parts: the bounded-tool boundary, identities and allow-lists are genuine, tested infrastructure for agents that do not yet exist.

### 6/10. ConcurGuard and recommendations
* ConcurGuard has nine real detectors (capacity, quantity, temporal, dependency, stale state, semantic alias, industry policy…) and benchmark tests.
* In the live path (`RecoveryPlanningApplicationService.detectConflicts`) every `ActionIntent` is built with empty resource claims, no capacity claim, no dependencies and a fabricated target (`"TARGET-FACILITY"` when absent). All three canned plans validate with "No conflicts" — the check is largely vacuous for them.
* `generateAlternativePlans` creates **three fixed templates** (Alpha/Beta/Gamma) with constant costs (45 000/78 000/32 000), durations and metrics; only the target node/material come from the disruption. They are not produced by agents, optimization or simulation. The Home page text claims different proposals ("Hsinchu Wafer Expedite, Kyoto Buffer Allocation, Air Freight Bypass").

### 8. Optimization
* `OrToolsRecoveryOptimizer` builds a genuine LP (`MPSolver` GLOP/SCIP: demand fulfilment, capacities, unmet-demand penalty).
* `OptimizationWorkspaceView` constructs the candidate options (`SUP-KYOTO`, `SUP-HSINCHU`, `PLANT-STUTTGART`, costs, capacities) as literals; only demand and objective weights come from the form.

### 16. Dashboards
* `ExecutiveWorkspaceView` shows **"Resilience Index 87.4 / 100"** and **"Revenue-at-Risk $4.2M"** as literals, and "Active Systemic Disruptions: 0" while the Home page, from the same running app, shows 2 active disruptions (different workspace id). These are invented KPIs.
* `SupplierIntelligenceService` sets OTIF to a constant (88.5 % single-source, 96.2 % otherwise) and lead-time variance to constants — fabricated metrics presented as supplier performance.

### 11–12. Governed execution and outcomes (Phases 9–10)
* Verified end to end (381 tests, PostgreSQL, browser): conversion, SoD, dependency enforcement, acknowledgement ≠ completion, unknown outcomes, restart recovery, plan-vs-actual, bounded reassessment.
* Limits relevant to the vision: only a SIMULATED adapter exists; verified outcomes reach the twin only as `RECOVERY_ACTION_COMPLETED` operational events, and **no monitoring rule consumes that event type** (`OperationalMonitoringService` reacts to delay, capacity, inventory and quality events only). The twin graph/ledger is never changed; nothing "learns".

### 9, 13, 15. Phases 11–13 (planning, collaboration, sustainability)
* `application.planning|collaboration|sustainability|platform` contain **zero references** to `DigitalTwin`, `SupplyChainGraph`, `BillOfMaterials`, `InventoryLedger`, `OperationalEvent` or `ExecutionWorkflow` (`grep`); sustainability references `MaterialId` only as an ID type. They are well-formed modules with their own tables and algorithms (e.g. forecasting SMA/WMA/exponential smoothing in `DemandPlanningService`) whose inputs are supplied by the operator, not derived from the twin.
* Supplier collaboration (RFQ, documents, quality) has no path into procurement plans, execution tasks or risk scoring.

### 17. Industry configuration
* The four industry packs are single classes (~90 lines each) and are read only by `IndustryConfigurationView`; no service takes behaviour from them (the `IndustryPolicyConflictDetector` in ConcurGuard exists but is fed by the same canned intents).

## What the product can answer today

| Question | Answer today |
|---|---|
| What is happening? | Home shows 2 seeded disruptions and a canned graph. Real events can be ingested (Phase 5/9) and raise alerts. |
| What is at risk / why / who is affected? | `Impact` computes downstream reachability on the in-memory twin from the disruption's node ids — genuinely graph-based, not BOM- or inventory-aware, no probabilities. |
| What if nothing is done? | Monte Carlo/DES run over parameters; their link to the twin was not verified. |
| Alternatives, best, why? | Three canned plans; OR-Tools over hard-coded candidates; explanations are templates. |
| Who approves, how executed, did it work? | **Yes and verified** — but only through simulated adapters, and the outcome does not change the twin. |
