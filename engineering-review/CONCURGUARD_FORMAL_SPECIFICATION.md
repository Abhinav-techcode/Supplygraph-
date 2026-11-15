# ConcurGuard — Formal Mathematical Specification & Conflict Resolution Engine

**Document ID:** `SG-ENG-REV-013`  
**Author:** Principal Systems Architect & AI Conflict Research Engineer  
**Date:** November 2026  
**Status:** Pre-Implementation Architectural Validation  

---

## 1. Conceptual Separation: Concurrency Layers

To avoid architectural confusion, SupplyGraph formally distinguishes between three distinct layers of conflict:

```
+───────────────────────────────────────────────────────────────────────────────────+
| LAYER 3: SEMANTIC PLAN-LEVEL CONFLICTS (ConcurGuard Engine)                       |
| Scope: Business time intervals [t_start, t_end), supply chain physical laws,      |
| multi-agent resource over-subscription, regulatory compliance, dependency DAGs.   |
| Example: Two plans created 2 hours apart reserving the same warehouse dock next   |
| Wednesday, or claiming 100% of a supplier's capacity across overlapping shifts.   |
+───────────────────────────────────────────────────────────────────────────────────+
                                          │
                                          ▼
+───────────────────────────────────────────────────────────────────────────────────+
| LAYER 2: PHYSICAL RESOURCE CONTENTION (Digital Twin Model)                        |
| Scope: Conservation of mass, finite machine hours, discrete lot inventory,        |
| minimum batch sizes, transportation payload envelope limits.                      |
| Example: Attempting to pack 25 metric tons of material into a 20-ton freight truck.|
+───────────────────────────────────────────────────────────────────────────────────+
                                          │
                                          ▼
+───────────────────────────────────────────────────────────────────────────────────+
| LAYER 1: DATABASE DATA CONCURRENCY (PostgreSQL / ACID Transactions)               |
| Scope: Millisecond-scale storage consistency, row locks, optimistic versioning    |
| (@Version), race conditions on concurrent SQL UPDATE/INSERT statements.          |
| Example: Two worker threads trying to update the same InventoryPosition row at    |
| the exact same millisecond. Handled via JPA optimistic locks & advisory locks.    |
+───────────────────────────────────────────────────────────────────────────────────+
```

---

## 2. Mathematical Formalization of Domain Primitives

### 2.1 TimeWindow ($\mathbb{T}$)
A continuous, half-open temporal interval on the real timeline $\mathbb{R}$:
$$\mathbb{T} = [t_{\text{start}}, t_{\text{end}}) \quad \text{where } t_{\text{start}}, t_{\text{end}} \in \text{Instant}, \; t_{\text{start}} < t_{\text{end}}$$

Two intervals $\mathbb{T}_1 = [s_1, e_1)$ and $\mathbb{T}_2 = [s_2, e_2)$ overlap ($\mathbb{T}_1 \cap \mathbb{T}_2 \neq \emptyset$) if and only if:
$$\text{Overlap}(\mathbb{T}_1, \mathbb{T}_2) \iff \max(s_1, s_2) < \min(e_1, e_2)$$

### 2.2 ResourceIdentity ($\mathcal{R}$) & Aliases
A resource identity $\mathcal{R}$ uniquely identifies a physical or operational entity:
$$\mathcal{R} = (\text{WorkspaceId}, \text{ResourceType}, \text{EntityId})$$
where $\text{ResourceType} \in \{\text{INVENTORY_LOT}, \text{MACHINE_TOOL}, \text{DOCK_BAY}, \text{TRANSPORT_LANE}, \text{SUPPLIER_CAPACITY}\}$.

Let $\mathcal{A}: \text{Alias} \to \mathcal{R}$ be an alias resolution mapping. If two actions reference distinct names (e.g., `"Courier-Express-Van-4"` and `"Internal-Plant-Transfer-Shuttle-4"`), $\mathcal{A}$ resolves them to the canonical resource identity $\mathcal{R}_0$.

### 2.3 QuantityClaim ($\mathcal{Q}$) & Unit Normalization
A claim on discrete material:
$$\mathcal{Q} = (\mathcal{R}, q, u, \mathbb{T}) \quad \text{where } q \in \mathbb{R}^+, \; u \in \text{UnitOfMeasure}$$

Let $\mathcal{N}(q, u, u_{\text{base}})$ be the deterministic unit conversion function:
$$\mathcal{N}(q, u, u_{\text{base}}) = q \times \text{ConversionFactor}(u \to u_{\text{base}})$$
*Constraint:* If $u$ cannot be converted to $u_{\text{base}}$ (e.g., metric tons to liters without density), a `DimensionalityException` is raised.

### 2.4 CapacityClaim ($\mathcal{C}$)
A claim on continuous throughput or rate-based resources:
$$\mathcal{C} = (\mathcal{R}, r, u_{\text{rate}}, \mathbb{T}) \quad \text{where } r \text{ is required rate per unit time}$$
*Example:* 15 machine-hours per day of a 24 machine-hour/day CNC tool.

### 2.5 ActionIntent ($I$)
A fully specified proposed action:
$$I = \left\langle \text{id}, \text{agentId}, \text{actionType}, \mathcal{R}_{\text{target}}, \mathbf{Q}, \mathbf{C}, \mathbb{T}, \nu_{\text{expected}}, \mathbf{D}, \mathbf{P}, \text{cost}, \text{reversibility} \right\rangle$$
where:
- $\mathbf{Q} = \{\mathcal{Q}_1, \dots, \mathcal{Q}_m\}$ is the set of quantity claims.
- $\mathbf{C} = \{\mathcal{C}_1, \dots, \mathcal{C}_k\}$ is the set of capacity claims.
- $\nu_{\text{expected}} \in \mathbb{N}$ is the expected digital twin state version.
- $\mathbf{D} = \{\text{id}_1, \dots, \text{id}_p\}$ is the set of prerequisite intent IDs (Dependency DAG).
- $\mathbf{P}$ is the set of industry domain preconditions.

---

## 3. Deterministic Semantics for the Seven Conflict Classes

Let $\mathcal{I} = \{I_1, I_2, \dots, I_n\}$ be a set of concurrently proposed action intents. ConcurGuard evaluates $\mathcal{I}$ against current digital twin state $S$ at version $\nu_{\text{current}}$.

### Conflict 1: Quantity Conflict ($C_{\text{QTY}}$)
For each resource $\mathcal{R}$ of type `INVENTORY_LOT`, let $A(\mathcal{R}, t)$ be the physical available unreserved balance at time $t$. A conflict exists if at any time $t$:
$$\sum_{I_k \in \mathcal{I}} \sum_{\mathcal{Q}_j \in I_k.\mathbf{Q} \mid \mathcal{Q}_j.\mathcal{R} = \mathcal{R} \land t \in \mathcal{Q}_j.\mathbb{T}} \mathcal{N}(\mathcal{Q}_j.q, \mathcal{Q}_j.u, u_{\text{base}}) > A(\mathcal{R}, t)$$

### Conflict 2: Capacity Conflict ($C_{\text{CAP}}$)
For each continuous resource $\mathcal{R}$ with maximum rated capacity $K_{\max}(\mathcal{R}, \mathbb{T})$, a conflict exists if over any interval $\mathbb{T}'$:
$$\sum_{I_k \in \mathcal{I}} \sum_{\mathcal{C}_j \in I_k.\mathbf{C} \mid \mathcal{C}_j.\mathcal{R} = \mathcal{R} \land \mathbb{T}' \subseteq \mathcal{C}_j.\mathbb{T}} \mathcal{C}_j.r > K_{\max}(\mathcal{R}, \mathbb{T}')$$

### Conflict 3: Temporal Overlap Conflict ($C_{\text{TEMP}}$)
For mutually exclusive resources $\mathcal{R}_{\text{mutex}}$ (e.g., a single physical dock bay or test equipment):
$$\exists I_a, I_b \in \mathcal{I} \; (a \neq b) \text{ s.t. } \mathcal{R}(I_a) = \mathcal{R}(I_b) = \mathcal{R}_{\text{mutex}} \land I_a.\mathbb{T} \cap I_b.\mathbb{T} \neq \emptyset$$

### Conflict 4: Shared-Resource Semantic Conflict ($C_{\text{SEM}}$)
Occurs when distinct intents use different names or aliases that resolve to the same underlying physical entity under $\mathcal{A}$:
$$\exists I_a, I_b \in \mathcal{I} \; (a \neq b) \text{ s.t. } \mathcal{A}(I_a.\mathcal{R}_{\text{target}}) = \mathcal{A}(I_b.\mathcal{R}_{\text{target}}) \land \text{ConflictingClaims}(I_a, I_b)$$

### Conflict 5: Dependency & Precondition Conflict ($C_{\text{DEP}}$)
For an intent $I_b$ with prerequisite dependency $I_a \in I_b.\mathbf{D}$:
$$C_{\text{DEP}}(I_b) \iff I_a \notin \text{ApprovedIntents} \lor \text{Status}(I_a) \in \{\text{REJECTED}, \text{FAILED}\} \lor I_a.\mathbb{T}.e > I_b.\mathbb{T}.s$$
(i.e., Action $A$ must complete before Action $B$ can start).

### Conflict 6: Stale-State Conflict ($C_{\text{STALE}}$)
Occurs when an agent computes an intent against an obsolete digital twin snapshot:
$$C_{\text{STALE}}(I) \iff I.\nu_{\text{expected}} < \nu_{\text{current}} \land \text{DeltaAffectsResource}(I.\mathcal{R}, I.\nu_{\text{expected}}, \nu_{\text{current}})$$

### Conflict 7: Industry Policy Conflict ($C_{\text{POL}}$)
Occurs when an intent violates a hard rule enforced by the active industry pack:
$$C_{\text{POL}}(I) \iff \exists \pi \in \text{IndustryPolicies} \text{ s.t. } \pi.\text{evaluate}(I, S) = \text{VIOLATION}$$
*Example:* Pharma cold-chain rule forbidding allocation of an unreleased lot with an active temperature deviation flag.

---

## 4. Arbitration State Machine & Precedence Rules

When a conflict set $\mathbf{C}$ is discovered, the arbitration engine executes deterministic resolution using strict precedence:

```
[Conflict Detected]
         │
         ▼
[1. Industry Hard Policy Violation?] ──────► YES ──► REJECT (Non-negotiable)
         │ NO
         ▼
[2. Stale State Drift?] ──────────────────► YES ──► REQUEST_REPLAN (Re-evaluate against v_current)
         │ NO
         ▼
[3. Temporal Dock / Tool Overlap?] ───────► YES ──► DELAY (Shift start time to next open slot)
         │ NO
         ▼
[4. Shift / Machine Capacity Exceeded?] ──► YES ──► RESCHEDULE (Move to alternate shift / day)
         │ NO
         ▼
[5. Partial Quantity Available?] ─────────► YES ──► MODIFY (Clamp quantity to available quota)
         │ NO
         ▼
[6. Unresolvable Structural Deficit] ─────► YES ──► REQUEST_REPLAN (Originating agent finds alternative)
```

### Deterministic Arbitration Precedence
1. **Hard Safety/Regulatory Invariants** take absolute precedence over operational efficiency.
2. **Older Approved Intents** take precedence over newly proposed intents (First-Committed, First-Served).
3. **Higher Priority Tier** (e.g., Medical Life-Saving Drug / Line-Down Emergency) takes precedence over Standard Stock Replenishment.
4. **Tie-Breaking:** If two competing proposals have identical priority, tie-breaking is strictly deterministic:
   $$\text{Precedence}(I_a, I_b) = \min(\text{EstimatedCost}) \to \min(\text{CreatedTimestamp}) \to \text{UUID.compareTo()}$$

---

## 5. Selective Replanning Algorithm & Termination Guarantees

```
Algorithm: ConcurGuard-Selective-Replanning
Input: Global Plan P = {I_1, ..., I_m}, New Conflict / Disruption Event E, Digital Twin S
Output: Repaired Feasible Plan P_repaired

1. Construct Dependency-Resource Graph G_P = (V_P, E_P):
   - V_P = P
   - Directed Edge (I_a, I_b) in E_P if:
       a) I_a in I_b.dependencies, OR
       b) I_a and I_b claim shared resource R over overlapping windows.

2. Identify Invalidation Roots:
   V_invalid = { I in P | HasConflict(I, S, E) == TRUE }

3. Compute Downstream Affected Subgraph via Breadth-First Search:
   V_affected = V_invalid U { I in P | exists path from any I_root in V_invalid to I in G_P }

4. Compute Preserved Subgraph:
   V_preserved = P \ V_affected
   FOR EACH I_p in V_preserved:
       ASSERT PreconditionsValid(I_p, S) == TRUE
       RetainResourceReservations(I_p)

5. Loop Bounded Re-orchestration:
   depth = 0; MAX_DEPTH = 5
   WHILE V_affected is not empty AND depth < MAX_DEPTH:
       depth = depth + 1
       TargetAgents = UniqueAgents(V_affected)
       NewProposals = InvokeSpecialistAgents(TargetAgents, Context(V_preserved, S))
       NewIntents = NormalizeToIndents(NewProposals)
       
       Conflicts = DetectConflicts(NewIntents, V_preserved, S)
       IF Conflicts is empty:
           P_repaired = V_preserved U NewIntents
           RETURN P_repaired
       ELSE:
           Arbitration = Arbitrate(Conflicts)
           ApplyModifications(NewIntents, Arbitration)
           V_affected = RemainingUnresolved(NewIntents)

6. IF depth >= MAX_DEPTH:
       EscalateToHumanSupervisor("Selective replanning exceeded max depth. Manual arbitration required.")
       RETURN FallbackSafePlan(V_preserved)
```

### Termination & Non-Oscillation Proof
- **Monotonicity:** An intent configuration state is hashed ($H = \text{SHA-256}(P)$). If state $H$ repeats, cycle detection halts execution immediately.
- **Strict Depth Bounding:** Hard-coded `MAX_DEPTH = 5` guarantees the replanner terminates in finite steps ($O(|V_P| + |E_P|)$) and never enters an infinite loop.

---

## 6. Fifteen Concrete Conflict Test Cases

The following 15 test cases form the mandatory automated test suite in `supplygraph-concurguard`:

| ID | Test Case Name | Given Context | Competing Agent Proposals | Detected Conflict Type | Expected Arbitration Decision |
|---|---|---|---|---|---|
| **TC-01** | Simultaneous Discrete Inventory Overbooking | Available inventory of Lithium Carbonate at Facility-A = 500 kg. | Agent 1 proposes 300 kg; Agent 2 simultaneously proposes 300 kg for overlapping window $[T_1, T_2)$. | `QUANTITY_CONFLICT` | **MODIFY / DELAY:** Agent 1 allocated 300 kg; Agent 2 clamped to 200 kg or requested to replan. |
| **TC-02** | Total Stock Exhaustion | Available unreserved stock of API Ibuprofen = 0 kg. | Procurement Agent proposes drawing 50 kg from local stock. | `QUANTITY_CONFLICT` | **REJECT:** Infeasible request on zero inventory. |
| **TC-03** | Continuous Machine Tool Over-subscription | Dry-room drying oven capacity = 24 hours/day. | Agent 1 claims 16 hours; Agent 2 claims 12 hours on same calendar day. | `CAPACITY_CONFLICT` | **RESCHEDULE:** Agent 1 scheduled Day 1; Agent 2 moved to open window on Day 2. |
| **TC-04** | Inbound Dock Bay Throughput Saturation | Warehouse dock has 2 physical bays. | 3 distinct logistics expedite shipments scheduled for arrival at exact window $[10:00, 11:00)$. | `CAPACITY_CONFLICT` | **DELAY:** Third shipment staggered to $[11:00, 12:00)$. |
| **TC-05** | Single Test Equipment Mutex Overlap | Specialized Battery Cell Cycler is single-unit exclusive. | Agent 1 and Agent 2 schedule simultaneous test runs. | `TEMPORAL_OVERLAP` | **DELAY:** Sequential queueing based on priority tier. |
| **TC-06** | Transport Lane Alias Conflict | Flight LH402 cargo bay weight limit = 5,000 kg. | Logistics proposes "Air Expedite Flight 402" (3,500 kg); Procurement proposes "Direct Courier LH-402" (2,500 kg). | `SEMANTIC_ALIAS` | **MODIFY:** Resolves aliases to flight LH402; total 6,000 kg exceeds payload; clamps lower priority freight. |
| **TC-07** | Internal Facility Alias Overbooking | Warehouse "Staging Area West" aliased as "Dock Bay 1 Floor". | Production stores 4 pallets in West; Logistics places 6 pallets in Dock 1. Max space = 6 pallets. | `SEMANTIC_ALIAS` | **RESCHEDULE / MODIFY:** Detects alias mapping; shifts 4 pallets to Overflow Bay. |
| **TC-08** | Precondition Sequence Inversion | Assembly Line 2 start requires raw material arrival at 08:00. | Shipment arrives at 11:00 due to weather; Production Agent starts line at 09:00. | `DEPENDENCY_CONFLICT` | **DELAY:** Production start automatically pushed to 11:30 post-unloading. |
| **TC-09** | Broken Dependency on Rejected Supplier | Sourcing proposal from uncertified Supplier-X is rejected. | Assembly proposal asserts parts from Supplier-X as prerequisite. | `DEPENDENCY_CONFLICT` | **REJECT / REQUEST_REPLAN:** Downstream proposal rejected because prerequisite cannot be satisfied. |
| **TC-10** | Stale Digital Twin State Version | Current Digital Twin version = $\nu_{15}$. | Agent generates proposal based on cached snapshot $\nu_{12}$ (prior to line breakdown). | `STALE_STATE_CONFLICT` | **REQUEST_REPLAN:** Proposal invalidated; agent must re-evaluate against $\nu_{15}$. |
| **TC-11** | Clean State Drift (No Overlap) | Current version = $\nu_{15}$. Proposal uses $\nu_{14}$. | Delta between $\nu_{14}$ and $\nu_{15}$ touched Facility-B; Proposal exclusively claims Facility-A. | *No Conflict* | **ALLOW:** State version delta has zero intersection with proposal's resource claims. |
| **TC-12** | Pharma Cold-Chain Quarantine Violation | Vaccine batch experienced 12°C excursion (limit 8°C); marked `QUARANTINED`. | Logistics Agent proposes packing and shipping quarantined batch. | `INDUSTRY_POLICY` | **REJECT:** Violates FDA/GMP excursion policy; quarantine cannot be shipped without QA release. |
| **TC-13** | EV Battery Precursor Grade Incompatibility | Automotive pack mandates battery-grade Lithium (>99.5% purity). | Procurement Agent proposes industrial-grade Lithium (98% purity) due to cost. | `INDUSTRY_POLICY` | **REJECT:** Fails EV pack material qualification policy. |
| **TC-14** | Selective Replanning Subgraph Isolation | 5-action plan: 2 transport, 1 packaging, 2 unaffected warehouse moves. | 1 transport action fails due to road closure. | `SELECTIVE_REPLAN` | **PRESERVE & REPLAN:** The 2 warehouse moves and packaging preserved; only failed transport replanned. |
| **TC-15** | Idempotent Proposal Re-submission | ConcurGuard receives duplicate submission of Intent $I_1$ with identical hash. | Same agent resends proposal due to network retry timeout. | *Idempotency Check* | **ALLOW (Idempotent):** Returns cached arbitration decision; zero duplicate resource deduction. |

