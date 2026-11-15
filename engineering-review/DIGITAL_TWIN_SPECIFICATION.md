# SupplyGraph — Digital Twin Canonical State Model & Simulation Engine Specification

**Document ID:** `SG-ENG-REV-014`  
**Author:** Senior Systems Architect & Discrete-Event Simulation Engineer  
**Date:** November 2026  
**Status:** Pre-Implementation Architectural Validation  

---

## 1. Canonical State Model Formalization

The SupplyGraph Digital Twin models an enterprise supply network as a formal state tuple $S$:
$$S = \langle \mathcal{W}, V, E, \mathcal{I}_{\text{pos}}, \mathcal{C}, \mathcal{O}, \tau, \nu \rangle$$

where:
1. $\mathcal{W}$ is the immutable `WorkspaceId`.
2. $V = V_{\text{org}} \cup V_{\text{fac}} \cup V_{\text{item}}$ is the set of graph vertices (Organizations, Facilities, Items).
3. $E = E_{\text{supply}} \cup E_{\text{bom}} \cup E_{\text{route}} \cup E_{\text{stage}}$ is the set of typed, directed graph edges.
4. $\mathcal{I}_{\text{pos}}$ is the complete inventory ledger across all facilities and lots.
5. $\mathcal{C}$ is the time-dependent capacity schedule across machines, docks, and transport lanes.
6. $\mathcal{O}$ is the set of active customer orders, production orders, and shipments.
7. $\tau \in \text{Instant}$ is the current simulation / operational clock timestamp.
8. $\nu \in \mathbb{N}^+$ is the monotonically increasing, atomic state version number.

---

## 2. Monotonic State Versioning & Snapshot Semantics

### 2.1 Atomic State Version Progression
State mutation is discrete and strictly serializable at the version level:
$$\nu_{k+1} = \nu_k + 1$$
Every state-changing operation (receiving a disruption, approving a recovery intent, executing a shipment) increments $\nu$.

### 2.2 Copy-on-Write (CoW) Branching for What-If Simulation
When agents or simulators evaluate hypothetical recovery scenarios:
1. The engine creates an **Immutable State Snapshot** $S_{\nu_k}$.
2. Simulation runs do not modify $S_{\nu_k}$; they create a transient **Delta Overlay** $\Delta S_{\text{sim}}$:
   $$S_{\text{sim}}(t) = S_{\nu_k} \oplus \Delta S_{\text{sim}}(t)$$
3. If the simulation branch is discarded, $\Delta S_{\text{sim}}$ is freed with zero garbage collection overhead on the production twin.
4. If a recovery plan is approved by a human operator, $\Delta S_{\text{sim}}$ is validated against current $\nu_{\text{current}}$ and committed atomically via an ACID database transaction.

---

## 3. Event Ordering & Deduplication Engine

Operational events (disruptions, shipping milestones, quality logs) arrive via the software ingestion port.

### 3.1 Total Event Ordering
To prevent race conditions where events arrive out-of-order, all operational events are assigned a strict total order:
$$e_a \prec e_b \iff (\text{eventTimestamp}(e_a) < \text{eventTimestamp}(e_b)) \lor (\text{eventTimestamp}(e_a) = \text{eventTimestamp}(e_b) \land \text{eventId}(e_a) < \text{eventId}(e_b))$$

### 3.2 Cryptographic Event Deduplication
Network retries from upstream systems can submit identical disruption notifications multiple times. The digital twin enforces idempotency via cryptographic content hashing:
$$\text{EventSignature} = \text{SHA-256}(\text{sourceSystem} \parallel \text{externalReferenceId} \parallel \text{eventType} \parallel \text{payloadBytes})$$
An in-memory LRU filter backed by a PostgreSQL unique index on `event_signature` discards duplicate events within a configurable deduplication window (default: 48 hours).

---

## 4. Double-Entry Inventory Accounting Ledger

To mathematically guarantee that inventory cannot be created or destroyed out of thin air (conservation of mass), SupplyGraph treats inventory accounting like financial double-entry bookkeeping.

```
+───────────────────────────────────────────────────────────────────────────────────+
|                           INVENTORY POSITION LEDGER ACCOUNTS                      |
+───────────────────────────+───────────────────────────+───────────────────────────+
| Account Name              | Meaning / Physical State  | Movement Rule             |
+───────────────────────────+───────────────────────────+───────────────────────────+
| `ON_HAND_AVAILABLE`       | Physically in warehouse,  | Decremented by reservation|
|                           | unreserved, released      | or consumption.           |
| `RESERVED_PRODUCTION`     | Physically present,       | Converted to scrap or     |
|                           | allocated to active batch | finished assembly.        |
| `RESERVED_SHIPMENT`       | Staged at dock for pickup | Moved to `IN_TRANSIT` on  |
|                           |                           | dispatch.                 |
| `IN_TRANSIT`              | Onboard carrier vehicle   | Moved to `ON_HAND` on     |
|                           | between facilities        | receiving dock sign-off.  |
| `QUARANTINED`             | Blocked by QA / excursion | Requires QA release to    |
|                           |                           | move to `AVAILABLE`.      |
+───────────────────────────+───────────────────────────+───────────────────────────+
```

### Mathematical Invariant: Conservation of Inventory
For any item $i$ and lot $l$ across the entire network:
$$\text{TotalPhysicalStock}(i, l) = \text{OnHandAvailable} + \text{ReservedProduction} + \text{ReservedShipment} + \text{InTransit} + \text{Quarantined}$$
Every transfer operation is an atomic pair of credit and debit entries:
$$\Delta \text{SourceAccount} = -q, \quad \Delta \text{DestinationAccount} = +q \implies \sum \Delta = 0$$

---

## 5. Bill-of-Materials (BOM) Graph Constraints

The production dependency structure is a directed edge set $E_{\text{bom}}$ connecting precursor items to parent assemblies:
$$(i_{\text{precursor}}, i_{\text{parent}}, \mu, \sigma) \in E_{\text{bom}}$$
where $\mu \in \mathbb{R}^+$ is the nominal consumption multiplier, and $\sigma \in [0, 1)$ is the historical scrap factor.

### 5.1 Strict DAG Invariant & Cycle Prevention
The BOM graph must be a strict **Directed Acyclic Graph (DAG)**:
$$\nexists \; \text{path } (i_1, i_2, \dots, i_k, i_1) \text{ in } E_{\text{bom}}$$
- *Enforcement:* When any BOM edge is registered or modified, the `BOMValidator` executes Tarjan's Strongly Connected Components algorithm (via `org.jgrapht`).
- If any cycle is detected, the operation is rejected immediately with `CircularBOMDependencyException`.

### 5.2 Scrap-Adjusted Component Requirement
To manufacture quantity $Q$ of parent item $i_{\text{parent}}$:
$$\text{RequiredPrecursorQuantity}(i_{\text{precursor}}) = \frac{Q \times \mu}{1 - \sigma}$$

---

## 6. Time-Dependent Capacity Modeling

Capacity is modeled as a discrete piecewise step function over time windows:
$$K(\mathcal{R}, t) = \sum_{w \in \text{CapacityWindows}(\mathcal{R})} K_w \cdot \mathbb{I}_{[w.s, w.e)}(t)$$
where $\mathbb{I}$ is the indicator function.

1. **Shift Adjustments:** Maintenance periods or holiday shutdowns are represented by explicit capacity windows with $K_w = 0$.
2. **Dynamic Degradation:** When an equipment failure disruption occurs, the disruption engine replaces the nominal capacity window with a degraded capacity window $K_{\text{degraded}} = \alpha \cdot K_{\text{nominal}}$ ($0 \le \alpha < 1$).

---

## 7. Deterministic Discrete-Event Simulation (DES) Engine

The simulation engine operates using a **deterministic virtual clock** and a priority event queue.

```
                    +─────────────────────────────────────────+
                    |        FUTURE EVENT LIST (FEL)          |
                    | Priority Queue sorted by (t_sim, seqNo) |
                    +────────────────────┬────────────────────+
                                         │
                                         ▼ Pop Next Event
                    +─────────────────────────────────────────+
                    |            SIMULATION CLOCK             |
                    |        Advances: tau = t_event          |
                    +────────────────────┬────────────────────+
                                         │
                                         ▼ Process Event
                    +─────────────────────────────────────────+
                    |          EVENT HANDLER DISPATCH         |
                    | - Material Departure / Arrival          |
                    | - Production Batch Start / Finish       |
                    | - Disruption Onset / Expiration         |
                    +────────────────────┬────────────────────+
                                         │
                                         ▼ State Mutated
                    +─────────────────────────────────────────+
                    |       DELTA OVERLAY ACCUMULATOR         |
                    |          Records KPIs & Delays          |
                    +─────────────────────────────────────────+
```

### Deterministic Tie-Breaking & Seeded Pseudo-Randomness
1. **Event Priority Queue:** Events are ordered by `(t_sim, eventPriority, sequenceNumber)`. If two events occur at the exact same microsecond, `sequenceNumber` (an atomic auto-increment counter assigned at insertion) guarantees bit-identical deterministic execution ordering across any JVM runtime.
2. **Pseudo-Random Number Generator (PRNG):** All stochastic variables (e.g., transit delay distributions or machine repair times) utilize `java.util.random.RandomGenerator` seeded with an explicit, recorded 64-bit integer (`simulationSeed`). Identical seeds yield 100% reproducible outcomes.

---

## 8. Prevention of Inconsistent State Transitions

The digital twin prevents invalid state transitions through a **Three-Phase Validation Pipeline**:
1. **Structural Precondition Check:** Validates entity existence, non-negative balances, and active workspace tenancy.
2. **Invariant Policy Evaluation:** Evaluates domain rules (BOM acyclicity, quarantine locks, half-open interval validity).
3. **Optimistic Version Verification:** Verifies that the entity's current in-memory version matches the version when the transaction began. If concurrent modifications occurred, the transaction aborts with `OptimisticLockException` and rolls back all changes cleanly.

