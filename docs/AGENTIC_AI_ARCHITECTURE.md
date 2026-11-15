# SupplyGraph — Agentic AI Architecture Specification

**Document ID:** `SG-DOC-012`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Approved Architectural Baseline  

---

## 1. Architectural Philosophy: Bounded, Proposal-Only Agents

SupplyGraph strictly rejects the naive "autonomous chatbot" paradigm. In an enterprise supply chain setting where a single errant purchase order or batch cancellation can cost millions of dollars and violate strict regulatory mandates (FDA, GxP, IATF 16949, ITAR), AI agents must operate under strictly bounded, verifiable, zero-trust constraints:

1. **Proposal-Only Mode:** Specialized agents may only construct candidate `ActionIntent`s. They have **zero** authority to mutate authoritative domain entities, commit ledger transactions, alter capacity calendars, or invoke real-world procurement APIs.
2. **Zero-Trust Information Access:** Agents inspect the state of the supply chain strictly through read-only inspection tools routed via a centralized Java `ToolExecutionBoundary`.
3. **Formal Quantitative Conflict Arbitration:** LLMs are **never** used for conflict detection, quantitative arbitration, or resource contention resolution. ConcurGuard provides deterministic, mathematically verifiable conflict resolution and selective replanning.
4. **Deterministic Reproducibility:** Offline deterministic fake AI providers ensure 100% reproducible test suites with simulated timeouts, provider outages, malformed JSON, and retries.

---

## 2. Specialized Agent Taxonomy

The SupplyGraph agentic layer comprises eight specialized agents, each bounded by strict responsibility contracts and allow-listed inspection tools:

| Agent Role | Package / Class | Bounded Responsibility | Allowed Tools | Candidate Actions |
|---|---|---|---|---|
| **COORDINATOR** | `CoordinatorAgent` | Ingests disruption incidents, establishes scope, coordinates specialized agents, and synthesizes overall recovery posture. | `query_inventory`, `query_bom`, `query_lane_status`, `query_capacity` | `EXPEDITE_PURCHASE_ORDER`, `SPLIT_PRODUCTION_BATCH` |
| **RISK_INTELLIGENCE** | `RiskIntelligenceAgent` | Monitors external disruption signals, supplier geopolitical vulnerability, port backlogs, and weather anomalies. | `query_lane_status`, `query_inventory` | `REROUTE_SHIPMENT` |
| **INVENTORY** | `InventoryAgent` | Evaluates safety stock thresholds, regional buffer balances, and inventory allocation trade-offs. | `query_inventory`, `query_bom` | `REALLOCATE_INVENTORY` |
| **PROCUREMENT** | `ProcurementAgent` | Evaluates multi-tier BOM precursor shortages, alternative supplier catalogs, and purchase order expedites. | `query_inventory`, `query_bom` | `EXPEDITE_PURCHASE_ORDER` |
| **LOGISTICS** | `LogisticsAgent` | Evaluates transportation lane transit times, carrier capacity, and multimodal freight rerouting (road, rail, air, ocean). | `query_lane_status` | `REROUTE_SHIPMENT` |
| **PRODUCTION** | `ProductionAgent` | Evaluates factory assembly line schedules, cleanroom availability, setup changeovers, and capacity windows. | `query_capacity`, `query_bom` | `RESERVE_CAPACITY` |
| **QUALITY** | `QualityAgent` | Enforces regulatory compliance (FDA CFR Part 11, cleanroom class ISO-14644, cold-chain temperature thresholds). | `query_inventory`, `query_bom` | `QUARANTINE_LOT`, `HOLD_SHIPMENT` |
| **RECOVERY_PLANNING** | `RecoveryPlanningAgent` | Synthesizes multi-objective Pareto trade-offs (cost vs. lead time vs. risk) across competing recovery plans. | `query_inventory`, `query_bom`, `query_lane_status`, `query_capacity` | `EXPEDITE_PURCHASE_ORDER`, `RESERVE_CAPACITY` |

---

## 3. The Multi-Agent Orchestration Pipeline

```
 [ Disruption Incident ]
          │
          ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ AgenticRecoveryOrchestrator                                 │
 │  1. Ingest incident & DigitalTwinSnapshot (vN)              │
 │  2. Initialize zero-trust ToolExecutionBoundary             │
 └────────────────────────┬────────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          │ Concurrent Investigation      │
          ▼                               ▼
 ┌───────────────────┐           ┌───────────────────┐
 │ Specialized Agent │           │ Specialized Agent │
 │  (e.g. Inventory) │           │  (e.g. Production)│
 └─────────┬─────────┘           └─────────┬─────────┘
           │ Read-Only Tools               │ Read-Only Tools
           ▼                               ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ ToolExecutionBoundary (Workspace, Allow-List, Rate-Limits)  │
 └────────────────────────┬────────────────────────────────────┘
                          │
                          ▼
            Candidate ActionIntents
                          │
                          ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ ConcurGuard Engine                                          │
 │  - 7 Deterministic Conflict Detectors                       │
 │  - Mathematical Precedence Arbitration                      │
 │  - Minimal Subgraph Selective Replanning Coordinator        │
 └────────────────────────┬────────────────────────────────────┘
                          │
     ┌────────────────────┴────────────────────┐
     │ No Conflicts                            │ Conflicts Detected
     ▼                                         ▼
 [ All Approved ]                  ┌────────────────────────┐
                                   │ Selective Replanning   │
                                   │ Only Affected Agents   │
                                   │ Preserves Clean Intent │
                                   └───────────┬────────────┘
                                               │ Converges
                                               ▼
                                  ┌─────────────────────────┐
                                  │ RecoveryProposal        │
                                  │ (Human Review Mode Only)│
                                  └─────────────────────────┘
```

---

## 4. Bounded Agent Contracts

### 4.1. Input Contract: `AgentInvestigationContext`
- `DisruptionIncident incident`: Title, severity, estimated downtime hours, affected nodes, affected materials.
- `DigitalTwinSnapshot snapshot`: Point-in-time immutable snapshot of BOM, graph, and double-entry inventory ledger.
- `Map<String, Object> parameters`: Dynamic scenario-specific tuning parameters.

### 4.2. Output Contract: `AgentProposal`
- `String proposalId`: Globally unique identifier.
- `AgentRole agentRole` & `AgentId agentId`: Strongly-typed source agent identity.
- `List<ActionIntent> intents`: Proposed intervention intents asserting formal claims.
- `List<String> evidence`: Verifiable empirical findings from snapshot inspection.
- `List<String> assumptions`: Explicit planning assumptions (e.g., supplier expedites available).
- `double confidence`: Quantified confidence score in range `[0.0, 1.0]`.
- `List<String> limitations`: Operational caveats (e.g., subject to customs clearance).
- `Instant proposedAt`: Timestamp of proposal creation.
