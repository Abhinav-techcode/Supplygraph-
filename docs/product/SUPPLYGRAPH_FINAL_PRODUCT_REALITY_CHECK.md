# SupplyGraph — Final Product Reality Check

**Date:** 2026-11-18  
**Repository:** `/Users/abhinavgupta/Desktop/Supplygraph`  
**Branch:** `product-realignment`  
**HEAD Commit:** `0463c2c` (Realign product with its vision: persisted twin, real graph, derived plans, no fabricated figures)  
**Reactor Baseline:** 14 Maven modules, 453 automated tests passing, 0 failures, 0 errors, 0 skipped.

---

## 1. Executive Summary

This Reality Check establishes the verified status of all 15 roadmap phases and subsystem capabilities in SupplyGraph. It compares the stated vision—*an industry-configurable, agentic supply-chain intelligence, digital twin, simulation, optimization, decision-support, and governed-execution platform*—against the actual codebase.

In the previous iteration (commit `0463c2c`), critical initial realignment was accomplished:
- Digital twin persistence and reload in PostgreSQL V2 tables (`supply_chain_nodes`, `transportation_lanes`, `materials`, `bom_entries`, `inventory_balances`) via `TwinGraphStore` and Flyway migration V16.
- Elimination of hard-coded SVG graphs in favour of dynamic `NetworkGraphViewService` rendering directly from twin state and active disruptions.
- Removal of fabricated KPI cards and static "Phase N" badges across views.
- Derivation of recovery candidates from twin topology via `RecoveryOptionDeriver`.

However, significant gaps remain between the current state and a production-grade enterprise product. This document catalogs each subsystem's exact implementation status using the non-negotiable classification framework.

---

## 2. Subsystem Capability Classification Matrix

| Capability / Subsystem | Verified Status | Evidence & Current Reality | Required Transformation |
|---|---|---|---|
| **Digital Twin Core & Persistence** | **IMPLEMENTED BUT INCOMPLETE** | `TwinGraphStore` persists nodes, lanes, BOM, and inventory balances to V2 tables in PostgreSQL. But `DemoDataInitializer` models ports as `Warehouse`, lacks explicit import paths, and has no "Load sample supply chain" action. | Model proper node types (`LogisticsHub`, `Supplier`, `ManufacturingSite`, `Warehouse`), build CSV/JSON twin import, decouple demo seeding from production profile. |
| **Graph Semantics & Topology** | **PARTIALLY IMPLEMENTED** | `SupplyChainGraph` models physical facilities and transportation lanes. `NetworkGraphViewService` computes layered DAG layouts dynamically. However, material-supply relationships (`SUPPLIES(material, share)`) are not explicitly typed edges. | Add material supply relations, multi-tier BOM-aware path traversal, and interactive graph enhancements (pan/zoom, path highlighting). |
| **BOM Propagation & Impact Analysis** | **PARTIALLY IMPLEMENTED** | `ImpactAnalysisApplicationService` traverses downstream reachability. However, BOM explosion to affected finished goods, customer orders, and Time-to-Survive (TTS) inventory runway uses constant heuristics ($85 price). | Propagate disruption through multi-tier BOM down to customer orders, calculate true inventory TTS from consumption rates, and eliminate price heuristics. |
| **Recommendation Pipeline** | **PARTIALLY IMPLEMENTED** | `RecoveryOptionDeriver` generates options from twin topology (alternate routes, suppliers, buffer release). However, proposals do not pass real capacity/resource claims into ConcurGuard detectors. | Feed real resource, quantity, capacity, and time-window claims into ConcurGuard; generate explanations from actual candidate data. |
| **ConcurGuard Conflict Detection** | **IMPLEMENTED BUT INCOMPLETE** | Real conflict detection engine with 6 detector classes (capacity, quantity, time window, alias, dependency, stale state). However, caller claims in `RecoveryPlanningApplicationService` are empty stubs. | Wire real claim extraction from candidate plans so detectors fire on genuine schedule/inventory contentions. |
| **Simulation & Monte Carlo** | **FULLY IMPLEMENTED AND VERIFIED** | Deterministic event priority DES (`DiscreteEventSimulationEngine`) and pseudo-random variate generator (`MonteCarloSimulationEngine`) are verified by 28 unit tests. | Ensure input parameters originate from the persisted twin rather than view defaults. |
| **OR-Tools Mathematical Optimization** | **FULLY IMPLEMENTED AND VERIFIED** | MPSolver integration with linear relaxation fallback (`OrToolsOptimizationEngine`) solves network flow and recovery allocation. | Connect live candidate sets directly from twin graph and inventory rather than static solver tables. |
| **Agentic AI (8 Agents)** | **HARDCODED / FABRICATED** | The 8 agents in `supplygraph-adapter-ai` return static/canned proposals. `AgenticRecoveryOrchestrator` is not actively driving the incident investigation lifecycle. | Rebuild agents as context-dependent specialists reading twin context through bounded tool boundaries; invoke orchestrator during investigations. |
| **Closed-Loop Execution & Feedback** | **IMPLEMENTED BUT INCOMPLETE** | Phase 10 execution state machine, governance, evidence signing, and restart recovery are fully verified. However, `RECOVERY_ACTION_COMPLETED` does not automatically mutate twin inventory/capacity. | Connect verified execution outcomes directly into `TwinGraphStore` / `InventoryLedger` to update physical twin state upon completion. |
| **Phase 11: Multi-Echelon Planning** | **PARTIALLY IMPLEMENTED** | MEIO, safety stock, and reorder planning algorithms exist, but pull from isolated repositories rather than the core digital twin. | Connect demand, inventory balances, lead times, and BOM directly from `DigitalTwinState`. |
| **Phase 12: Sustainability & Traceability** | **PARTIALLY IMPLEMENTED** | Carbon emission models and batch/lot genealogy exist, but rely on static fixtures rather than linking to twin nodes, lanes, and shipments. | Bind emissions factors and genealogy records to twin transportation lanes and material batches; distinguish measured vs estimated emissions. |
| **Phase 13: Supplier Collaboration** | **PARTIALLY IMPLEMENTED** | Supplier portal, RFQ, quotation, and quality workflows exist, but do not feed capacity confirmations or ASNs back into core planning/risk. | Feed supplier capacity updates and delivery commitments directly into twin capacity calendars and incident recovery derivers. |
| **Authentication & Authorization** | **UI ONLY / PLACEHOLDER** | `UiPrincipalFilter` provides a demo session switcher. `WorkspaceAccessGuard` is only applied to ~10 classes. No genuine Spring Security authentication. | Implement real Spring Security authentication (OIDC / Form-based) and enforce `WorkspaceAccessGuard` server-side across ALL application services. |
| **Navigation & User Experience** | **PARTIALLY IMPLEMENTED** | 50+ flat navigation links in `MainLayout`. Multiple duplicate command centres and role dashboards. No single guided incident workspace. | Redesign navigation into 9 task-oriented areas; build ONE guided incident workspace with a unified end-to-end stepper. |
| **Production Deployment & DevOps** | **NOT IMPLEMENTED** | Application runs on local H2 demo script (`run-demo.sh`) or local PostgreSQL. No production profile (`application-prod.yml`), Dockerfile, or cloud configuration. | Create production configuration, multi-stage Dockerfile, docker-compose, health check endpoints, and deployment guide. |

---

## 3. Metric Provenance Audit Summary

| Metric / KPI Area | Current Reality | Status | Target Action |
|---|---|---|---|
| Active Disruptions | Counted from `DisruptionRepositoryPort.findByWorkspaceIdAndStatus` | REAL | Maintain |
| Impacted Nodes & Lanes | Computed via `graph.getDownstreamReachableNodes()` | REAL | Expand to include BOM finished products |
| On-Hand Inventory | Aggregated from `InventoryLedger.getAllBalances()` | REAL | Maintain |
| Plan Cost Estimate | Sum of lane `costPerKg` across transit duration | FLAWED SEMANTICS | Distinguish fixed freight vs variable weight-based costs |
| Time-to-Survive (TTS) | Static estimate in `AdvancedResilience` heuristic | PARTIAL | Compute dynamically from inventory / daily consumption rate |
| Single Point of Failure (SPOF) | Heuristic: `alternatives == 0 \|\| downstream > 3` | INCOMPLETE | Replace with graph articulation point analysis |
| Supplier Lead Time Variance | Fixed constants (88.5 / 96.2, 6.5 / 1.8) | FABRICATED | Compute from operational events or show `No data` |
| Carbon Footprint / ESG | Hardcoded calculation in view | FABRICATED | Calculate from lane transport mode × distance × emission factor |

---

## 4. Immediate Execution Plan

1. **Phase 1:** Correct domain model node/lane semantics; implement CSV/JSON twin import and "Load sample supply chain" action.
2. **Phase 2:** Upgrade graph traversal for multi-tier BOM explosion and true articulation-point SPOF detection.
3. **Phase 3:** Populate real ConcurGuard claims in recovery options and generate dynamic explanations.
4. **Phase 4:** Rebuild the 8 specialist agents as context-dependent actors behind the tool boundary.
5. **Phase 5:** Wire verified execution evidence to mutate digital twin inventory and capacity.
6. **Phase 6:** Connect Planning (Phase 11), Sustainability (Phase 12), and Collaboration (Phase 13) to the shared digital twin.
7. **Phase 7:** Implement Spring Security authentication and enforce workspace authorization server-wide.
8. **Phase 8:** Redesign Vaadin navigation into 9 task areas and construct the unified Guided Incident Workspace.
9. **Phase 9:** Eliminate remaining view-level fallback data and finalize Metric Provenance Register.
10. **Phase 10 & 11:** Execute complete acceptance test journey, build production profile, Docker container, and deployment guide.

