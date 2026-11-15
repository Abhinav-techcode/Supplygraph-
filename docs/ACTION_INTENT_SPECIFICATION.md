# SupplyGraph — ActionIntent Formal Specification

**Document ID:** `SG-DOC-014`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Approved Formal Specification  

---

## 1. Intent Model Definition

An `ActionIntent` is the authoritative, immutable intermediate representation through which bounded specialized agents express candidate interventions in response to a supply chain disruption.

Every `ActionIntent` carries explicit quantitative claims, temporal bounds, and causal dependencies, allowing ConcurGuard to evaluate plan validity and multi-agent resource contention without ambiguity.

---

## 2. Structural Field Specification

```java
public record ActionIntent(
        IntentId id,
        String incidentId,
        AgentId agentId,
        ActionType actionType,
        String targetEntityId,
        List<ResourceClaim> resourceClaims,
        Quantity quantity,
        CapacityClaim capacityClaim,
        CapacityWindow timeWindow,
        String location,
        Set<IntentId> dependencies,
        List<String> preconditions,
        StateVersion expectedStateVersion,
        List<String> evidenceReferences,
        List<String> assumptions,
        BigDecimal estimatedCost,
        double riskScore,
        boolean reversible,
        boolean requiresHumanApproval
)
```

| Field | Type | Invariant / Validation Rule | Semantics |
|---|---|---|---|
| `id` | `IntentId` | Mandatory, non-null, non-blank | Unique intent identifier (e.g., `INT-INV-001`). |
| `incidentId` | `String` | Mandatory, non-null | Links the proposal to the triggering `DisruptionIncident`. |
| `agentId` | `AgentId` | Mandatory, non-null | Strongly-typed role identity of the proposing agent. |
| `actionType` | `ActionType` | Mandatory, non-null | Standardized intervention category (see Section 3). |
| `targetEntityId` | `String` | Mandatory, non-null | Target entity (e.g., PO number, stock SKU, line ID, lane ID). |
| `resourceClaims` | `List<ResourceClaim>` | Immutable list (may be empty) | Explicit physical claims asserted over inventory or assets. |
| `quantity` | `Quantity` | Optional (`BigDecimal` + `UnitOfMeasure`) | Physical material quantity involved in the action. |
| `capacityClaim` | `CapacityClaim` | Optional | Capacity claim on a machine/line over a `CapacityWindow`. |
| `timeWindow` | `CapacityWindow` | Optional | Temporal validity $[t_{start}, t_{end})$ of the proposed action. |
| `location` | `String` | Default `"GLOBAL"` | Regional or facility scope. |
| `dependencies` | `Set<IntentId>` | Immutable set | Parent intent IDs that must execute prior to this intent. |
| `preconditions` | `List<String>` | Immutable list | Explicit operational preconditions. |
| `expectedStateVersion`| `StateVersion` | Mandatory, default $v_0$ | The Digital Twin snapshot version against which the agent planned. |
| `evidenceReferences` | `List<String>` | Immutable list | Citations to inspection tool queries or ledger accounts. |
| `assumptions` | `List<String>` | Immutable list | Key planning assumptions. |
| `estimatedCost` | `BigDecimal` | Non-negative, default 0 | Projected operational expense of the intervention. |
| `riskScore` | `double` | Range $[0.0, 1.0]$ | Quantified failure/execution risk score. |
| `reversible` | `boolean` | Default `true` | Indicates whether the action can be rolled back. |
| `requiresHumanApproval`| `boolean` | Default `false` | Flags high-impact regulatory or financial thresholds. |

---

## 3. Standard Action Types (`ActionType`)

- `EXPEDITE_PURCHASE_ORDER`: Accelerates delivery date of an existing PO or issues an emergency spot order.
- `REROUTE_SHIPMENT`: Diverts transport freight to an alternative carrier, transport mode, or routing corridor.
- `SPLIT_PRODUCTION_BATCH`: Divides a manufacturing lot into smaller batches to prioritize critical customer orders.
- `REALLOCATE_INVENTORY`: Drawdown or transfers physical safety stock buffer between network nodes.
- `RESERVE_CAPACITY`: Books throughput units on a factory manufacturing line or cleanroom over a time window.
- `QUARANTINE_LOT`: Places a batch under quality/inspection hold due to excursion or defect alerts.
- `SUBSTITUTE_MATERIAL`: Activates an approved alternate precursor component in the BOM.
- `HOLD_SHIPMENT`: Halts transit of goods failing quality or regulatory documentation checks.
- `AUTHORIZE_OVERTIME_SHIFT`: Allocates overtime capacity to clear production bottlenecks.
- `EMERGENCY_BUFFER_DISPATCH`: Authorizes emergency dispatch from central strategic reserve.

---

## 4. Formal Resource Claims (`ResourceClaim`)

A `ResourceClaim` asserts ownership over a physical asset. Claims are defined by:
1. `ResourceIdentity`: Normalized `(resourceType, canonicalId, facilityId)` tuple resolving aliases.
2. `ClaimType`:
   - `EXCLUSIVE`: Exclusive utilization of machine, line, or lot during the window.
   - `SHARED`: Rate-shared throughput utilization (e.g., 20 units/hour out of 100/hour).
   - `CONSUMPTIVE`: Permanently decrements available physical inventory balances.
3. `claimedQuantity`: The exact `Quantity` claimed.
4. `claimWindow`: The active time interval $[t_{start}, t_{end})$.
