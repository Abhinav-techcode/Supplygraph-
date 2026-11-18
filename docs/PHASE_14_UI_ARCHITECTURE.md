# SupplyGraph UI Architecture & Persona Workspaces (Phase 14)
## Persona Cockpits, Layout Hierarchies & Contextual Navigation

**Status:** APPROVED & IMPLEMENTED  
**Date:** November 18, 2026  
**Architecture Lead:** Principal Enterprise Software Architect  

---

## 1. Architectural Strategy

SupplyGraph employs a **Role-Tailored Persona Architecture**. Rather than forcing all enterprise users into a single monolithic dashboard, the system provides 9 tailored operational cockpits plus a unified cross-domain command center.

Each workspace extends `WorkspaceViewBase`:
- Automatically enforces server-side workspace isolation through `SecurityContext.getCurrentUser()`.
- Renders persona-specific metadata badges and headers.
- Synthesizes domain data exclusively through hexagonal application services (zero direct persistence entity leakage).

---

## 2. Persona Workspaces Matrix

| Persona | Route | Primary Responsibilities | Injected Services |
|---|---|---|---|
| **C-Level Executive / VP** | `/workspace/executive` | Strategic risk, resilience index, financial exposure, ESG rating | `DisruptionApplicationService`, `ExplainableRecommendationService`, `MultiObjectiveParetoService`, `SupplierSustainabilityService` |
| **Supply Chain Planner** | `/workspace/planner` | Aggregate forecasting, MEIO safety stocks, finite capacity schedule | `DemandPlanningService`, `MultiEchelonInventoryService`, `ProductionPlanningService`, `PlanningScenarioService` |
| **Procurement Manager** | `/workspace/procurement` | Sourcing tenders, quotation awards, PO commitments, multi-sourcing split | `ProcurementPlanningService`, `RfqCollaborationService`, `SupplierPortalService`, `OrderCollaborationService` |
| **Plant Production Manager**| `/workspace/production` | Machine job sequencing, changeover minimization, OEE yield | `ProductionPlanningService`, `SupplierQualityCollaborationService` |
| **Logistics & Freight Manager**| `/workspace/logistics` | Carrier waybill telemetry, ASNs, modal trade-offs (Air/Road/Rail/Sea), freight GHG | `SupplyChainNetworkPlanningService`, `CarbonAccountingService`, `OrderCollaborationService` |
| **Quality & Compliance Director**| `/workspace/quality` | 8D problem solving, root-cause ICA/PCA, lot genealogy recall readiness | `SupplierQualityCollaborationService`, `ProductTraceabilityService`, `PartnerDocumentExchangeService` |
| **Data Scientist / Analyst** | `/workspace/analyst` | Monte Carlo risk, discrete-event simulation, Pareto frontiers, What-If hypotheses | `WhatIfAnalysisService`, `SensitivityAnalysisService`, `MultiObjectiveParetoService`, `SimulationApplicationService` |
| **External Supplier Partner** | `/workspace/supplier` | Order fulfillment, delivery promise acknowledgment, ASN dispatch, RFQ tender bids | `OrderCollaborationService`, `RfqCollaborationService`, `PartnerDocumentExchangeService`, `SupplierPortalService` |
| **Platform Administrator** | `/workspace/admin` | User RBAC accounts, audit governance log, transactional outbox, telemetry | `UserManagementService`, `AuditGovernanceService` |

---

## 3. Unified Command Centre & Global Search

Located at route `/command-center`:
- **Universal Multi-Domain Query Engine:** Evaluates search tokens across:
  1. Systemic Disruptions (`Disruption`)
  2. Purchase Order Commitments (`PoCollaborationRecord`)
  3. Strategic RFQs (`RfqDocument`)
  4. 8D Quality Non-Conformances (`QualityIncident8D`)
  5. Traceability Lots & Batches (`TraceabilityNode`)
- **Actionable Jump Routes:** Every search hit provides a direct link to the corresponding detailed workspace view.
- **One-Click Persona Launcher:** Immediate shortcuts to all 9 role workspaces.

