# SupplyGraph — ConcurGuard Implementation Guide

**Document ID:** `SG-DOC-015`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Approved Implementation Reference  

---

## 1. Engine Architecture

ConcurGuard is the deterministic conflict-aware agent coordination and arbitration engine of SupplyGraph. It guarantees that multi-agent recovery plans are formally validated, free of resource collisions, deadlocks, and regulatory violations, without relying on stochastic LLMs for mathematical arbitration.

```
 ActionIntents from Specialized Agents + DigitalTwinSnapshot
                        │
                        ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ ConcurGuardConflictDetector                                 │
 │  ├── QuantityConflictDetector                               │
 │  ├── CapacityConflictDetector                               │
 │  ├── TemporalConflictDetector                               │
 │  ├── SemanticAliasConflictDetector                          │
 │  ├── DependencyConflictDetector                             │
 │  ├── StaleStateConflictDetector                             │
 │  └── IndustryPolicyConflictDetector                         │
 └──────────────────────┬──────────────────────────────────────┘
                        │ Conflicts List
                        ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ ConcurGuardArbitrator (Deterministic Precedence Hierarchy)  │
 │  1. Safety & Regulatory Holds (CRITICAL)                    │
 │  2. Deadlock Cycles (ERROR)                                 │
 │  3. Stale-State Violations (ERROR)                          │
 │  4. Scarcity Contention (Pareto: RiskScore > Cost > Time)   │
 └──────────────────────┬──────────────────────────────────────┘
                        │ Arbitration Decisions
                        ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ SelectiveReplanningCoordinator                              │
 │  - Constructs IntentDependencyGraph                         │
 │  - Isolates Minimal Affected Subgraph (ReplanningScope)     │
 │  - Preserves Unaffected Independent Subgraphs               │
 │  - Triggers Targeted Replanning on Affected Agents Only     │
 │  - Halts Loop on Convergence or Max Iterations              │
 └─────────────────────────────────────────────────────────────┘
```

---

## 2. The Seven Conflict Classes & Detection Semantics

| Conflict Class | Detector Class | Detection Logic | Severity |
|---|---|---|---|
| **1. Quantity Allocation Conflict** | `QuantityConflictDetector` | Identifies competing consumptive claims on the same resource where $\sum Q_{claimed} > Q_{available}$, or when multiple agents claim > 75% of buffer. | `WARNING` / `ERROR` |
| **2. Capacity Overcommit Conflict** | `CapacityConflictDetector` | Detects overlapping capacity claims on the same resource ID where temporal windows $[t_1, t_2)$ intersect and aggregate rate exceeds machine capacity. | `ERROR` |
| **3. Temporal / Sequencing Conflict** | `TemporalConflictDetector` | Detects causal violations where dependent intent $B$ (e.g., assembly) has a start time earlier than the completion time of predecessor intent $A$ ($t_{start}^B < t_{end}^A$). | `ERROR` |
| **4. Semantic & Resource Alias Conflict** | `SemanticAliasConflictDetector` | Resolves alternate naming schemes (e.g., `"COATER-A"` vs `"LINE-COAT-01"`) via `ResourceAliasRegistry` and flags hidden resource collisions. | `ERROR` |
| **5. Dependency Invalidation Conflict** | `DependencyConflictDetector` | Builds intent dependency graph, detects cyclic deadlock dependencies ($A \rightarrow B \rightarrow A$), and flags missing prerequisites. | `CRITICAL` |
| **6. Stale-State Conflict** | `StaleStateConflictDetector` | Verifies that intent's `expectedStateVersion` matches the snapshot `version()`. Rejects plans computed against obsolete state versions ($v_{expected} < v_{actual}$). | `ERROR` |
| **7. Industry Policy & Regulatory Conflict** | `IndustryPolicyConflictDetector` | Evaluates regulatory violations: shipping quarantined lots, exceeding shelf-life, cold-chain excursion holds, and cleanroom class mismatches. | `CRITICAL` |

---

## 3. Deterministic Arbitration Precedence

When conflicts are detected, `ConcurGuardArbitrator` executes deterministic arbitration following strict priority rules:

1. **Precedence Tier 1 (Safety & Policy Precedence):**
   - Mandatory compliance holds, FDA quarantine orders, and safety interlocks always win.
   - Any competing intent attempting to allocate, move, or process a quarantined lot is marked `REJECTED`.
2. **Precedence Tier 2 (Deadlock & Cycle Invalidation):**
   - In dependency cycles, the intent with the higher risk score is marked `REJECTED`, breaking the cycle cleanly.
3. **Precedence Tier 3 (Stale State Rejection):**
   - Intents planned against outdated state versions receive `REVISE_PROPOSAL` / `REQUEST_REPLANNING`.
4. **Precedence Tier 4 (Quantitative Scarcity Contention):**
   - For rival claims on scarce capacity or inventory, the intent with the lower `riskScore` wins (`ALLOW`), while the competing intent is awarded `REQUEST_REPLANNING` with a suggested alternative window or quantity.
   - Ties are broken deterministically by lexicographical `IntentId` comparison.
