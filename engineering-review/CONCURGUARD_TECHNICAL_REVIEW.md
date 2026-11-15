# ConcurGuard — Technical Specification & Conflict Resolution Engine

**Document ID:** `SG-ENG-REV-007`  
**Author:** Principal Systems Architect & AI Conflict Research Engineer  
**Date:** November 2026  
**Status:** ConcurGuard Technical Baseline  

---

## 1. Why ConcurGuard: The Plan-Level Semantic Conflict Challenge

### 1.1 The Limitation of Traditional Database Concurrency
Traditional database isolation (ACID transactions, Row-Level Locking, Optimistic Locking via `@Version`, Two-Phase Locking) operates at the **storage layer**:
- Database locks prevent two concurrent threads from writing to the same database row at millisecond scale.
- **Database locks cannot detect semantic plan conflicts:** If the Procurement Agent proposes booking 100% of Supplier X's capacity for next Wednesday, and 30 seconds later the Production Agent independently proposes reserving 80% of the same supplier's capacity for next Wednesday, both database transactions commit successfully!
- The resulting combined recovery plan is physically impossible, leading to catastrophic plant starvation or line-stops.

### 1.2 ConcurGuard Defined
**ConcurGuard** is a domain-level coordination engine that treats recovery proposals as **temporal, multi-dimensional resource reservations**. It validates whether a set of independently generated agent action intents can be synthesized into a mutually feasible, conflict-free global execution plan.

```
                    AGENT PROPOSALS (Independent & Concurrent)
                      [Procurement]    [Logistics]    [Production]
                            │               │               │
                            ▼               ▼               ▼
            +───────────────────────────────────────────────────────────────+
            |                  CONCURGUARD INTENT REGISTRY                  |
            |        Normalized Claims: Resource, Quantity, TimeWindow      |
            +───────────────────────────────┬───────────────────────────────+
                                            │
                                            ▼
            +───────────────────────────────────────────────────────────────+
            |                 7-DIMENSIONAL CONFLICT DETECTOR               |
            |   Quantity | Capacity | Temporal | Semantic | Dependency      |
            |            Stale-State | Industry Domain Policy               |
            +───────────────────────────────┬───────────────────────────────+
                                            │
                                            ▼
            +───────────────────────────────────────────────────────────────+
            |                 ARBITRATION & DECISION ENGINE                 |
            |     ALLOW | DELAY | RESCHEDULE | MODIFY | REJECT | REPLAN     |
            +───────────────────────────────┬───────────────────────────────+
                                            │
                          ┌─────────────────┴─────────────────┐
                          │ Conflict Detected                 │ All Valid
                          ▼                                   ▼
            +───────────────────────────+       +───────────────────────────+
            |    SELECTIVE REPLANNER    |       |   FEASIBLE GLOBAL PLAN    |
            | Minimal Subgraph Repair   |       | Sent to Optimizer / Sim   |
            +───────────────────────────+       +───────────────────────────+
```

---

## 2. The Seven Conflict Classes

ConcurGuard inspects action intents across 7 distinct dimensions:

### 2.1 Quantity Conflicts
Occurs when the sum of claimed quantities of a discrete material or inventory lot exceeds the physical available unreserved balance during an overlapping window:
$$\sum_{i \in \text{Intents}} \text{ClaimedQuantity}(i, R, t) > \text{AvailableQuantity}(R, t)$$
*Example:* Both EV Battery Pack-A and Pack-B lines claim 300 kg of a remaining 400 kg lot of battery-grade lithium hydroxide.

### 2.2 Capacity Conflicts
Occurs when continuous resource throughput (e.g., machine hours, cleanroom square footage, loading dock throughput) exceeds maximum rated capacity:
$$\sum_{i \in \text{Intents}} \text{RequiredRate}(i, C, [t_1, t_2)) > \text{MaxCapacity}(C, [t_1, t_2))$$
*Example:* Two expedite proposals schedule simultaneous deliveries requiring 4 inbound dock doors when the facility has only 2 operational bays.

### 2.3 Temporal Overlap Conflicts
Occurs when mutually exclusive actions are scheduled during intersecting time intervals:
$$[t_{\text{start}}^A, t_{\text{end}}^A) \cap [t_{\text{start}}^B, t_{\text{end}}^B) \neq \emptyset$$
*Example:* Scheduled maintenance on a semiconductor photolithography stepper overlaps with an expedited wafer lot run.

### 2.4 Semantic (Shared-Resource) Conflicts
Occurs when differently named actions from distinct agents implicitly consume the exact same underlying asset under different aliases:
*Example:* Logistics Agent proposes "Expedited Air Freight via Flight LH402" while Procurement Agent proposes "Supplier Direct Courier via Flight LH402" for different components, exceeding the aircraft cargo payload weight envelope.

### 2.5 Dependency & Precondition Conflicts
Occurs when Action $B$ asserts Action $A$ as an execution prerequisite, but Action $A$ has failed, been delayed past $B$'s latest start time, or rejected:
$$\text{Precondition}(B) = \text{Completed}(A) \quad \text{where} \quad \text{Status}(A) \in \{\text{REJECTED}, \text{DELAYED}\} \implies \text{Conflict}$$
*Example:* Assembly line startup scheduled for 08:00 requires chemical delivery at 07:00, but transit delays shift delivery to 11:00.

### 2.6 Stale-State Conflicts
Occurs when an agent computes a proposal against snapshot `stateVersion = K`, but prior approved actions or new disruption events have advanced the digital twin to `stateVersion = K + N`:
$$\text{Intent}.\text{expectedStateVersion} < \text{DigitalTwin}.\text{currentStateVersion}$$
ConcurGuard mandates re-evaluating the delta impact before allowing execution.

### 2.7 Industry-Specific Policy Conflicts
Occurs when an action violates domain-specific regulatory, safety, or compliance rules:
*Example:* Pharmaceutical GMP rule mandating that a cold-chain batch undergoing a temperature excursion above 8°C cannot be released to packaging without a completed 48-hour stability testing sign-off.

---

## 3. Arbitration Decision State Machine

ConcurGuard resolves detected conflicts through deterministic arbitration rules:

```
                              [Intent Submitted]
                                      │
                                      ▼
                             [Conflict Check]
                                 /         \
                       No Conflict          Conflict Detected
                             /               \
                            ▼                 ▼
                        [ALLOW]      [Arbitration Engine]
                                      /   |   |   |    \
                                     /    |   |   |     \
                                    ▼     ▼   ▼   ▼      ▼
                                [DELAY][RESCHEDULE][MODIFY][REJECT][REQUEST_REPLAN]
```

1. **`ALLOW`:** The intent is conflict-free and compliant. Its resource claims are provisionally registered.
2. **`DELAY`:** A temporal conflict is resolved by shifting the intent start time forward into the next available resource window without violating downstream deadlines.
3. **`RESCHEDULE`:** The intent is moved to an alternate non-contiguous time slot or secondary shift.
4. **`MODIFY`:** The intent quantity is clamped to the available quota, or routed to a secondary facility.
5. **`REJECT`:** The intent violates unresolvable hard constraints (e.g., GMP safety violation) and is permanently discarded.
6. **`REQUEST_REPLANNING`:** The intent cannot be resolved through simple adjustment. The originating agent is instructed to generate alternative approaches.

---

## 4. The Selective Replanning Algorithm

When a disruption or conflict invalidates part of a multi-action plan, naive systems scrap the entire plan and start over. This causes severe churn, cancels valid contracts, and explodes LLM computation costs.

SupplyGraph implements **Selective Replanning**:

### 4.1 Algorithmic Procedure
1. **Construct Intent Dependency Graph:** Let $G_I = (I, E_I)$ where nodes $I$ are action intents and directed edges $(i_a, i_b) \in E_I$ represent dependencies or shared claimed resources.
2. **Identify Invalidation Source:** Let $i_{\text{invalid}} \in I$ be the conflicting or disrupted intent.
3. **Compute Affected Subgraph:**
   $$\text{Affected}(G_I) = \{ i \in I \mid \text{path}(i_{\text{invalid}}, i) \text{ exists in } G_I \}$$
4. **Preserve Valid Unaffected Intents:**
   $$\text{Preserved}(G_I) = I \setminus \text{Affected}(G_I)$$
   All intents in $\text{Preserved}(G_I)$ retain their reservations, contracts, and scheduled times.
5. **Targeted Agent Re-orchestration:** Only the agents responsible for intents in $\text{Affected}(G_I)$ are invoked to generate replacement proposals.
6. **Incremental Re-arbitration:** The newly generated replacement intents are checked against the retained claims of $\text{Preserved}(G_I)$.
7. **Audit Record:** A diff log is published recording:
   - Preserved Actions: $N_{\text{preserved}}$
   - Replaced Actions: $N_{\text{replaced}}$
   - Rejected Actions: $N_{\text{rejected}}$

---

## 5. Research Honesty & Prior Art Positioning

### 5.1 Scientific Rigor
The blueprint explicitly commands that ConcurGuard must not be marketed as unverified "academic novelty" without rigorous literature positioning:
- **Prior Art Grounding:** ConcurGuard builds upon proven principles in:
  - Multi-Agent Resource Allocation (MARA)
  - Hierarchical Task Network (HTN) Planning
  - Simple Temporal Networks with Uncertainty (STNU)
  - Distributed Constraint Satisfaction Problems (DisCSP)
- **Measurable Benchmark Experiments:** ConcurGuard's efficacy will be benchmarked directly against:
  1. *Uncoordinated Multi-Agent Baseline:* Independent agents generating actions without coordination.
  2. *Naive Pessimistic Locking Baseline:* Standard database row locks on inventory positions.
  3. *Full Plan Regeneration Baseline:* Scrap-and-rebuild replanning without selective subgraph preservation.
- **Reported Metrics:** Feasible plan percentage, conflict discovery precision/recall, compute latency, LLM token efficiency, and percentage of preserved valid actions.

