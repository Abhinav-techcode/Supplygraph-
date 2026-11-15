# SupplyGraph — Selective Replanning Specification

**Document ID:** `SG-DOC-016`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Approved Algorithmic Specification  

---

## 1. The Selective Replanning Problem

In a complex multi-agent supply chain recovery scenario, when a conflict is detected between two agents (e.g., competing capacity claims on a single coating machine), a naive architecture regenerates the **entire** plan from scratch across all agents.

Full plan regeneration introduces severe operational defects:
1. **Instability & Plan Churn:** Clean, non-conflicting proposals (e.g., an independent purchase order or road transport reroute 500 miles away) are unnecessarily discarded.
2. **Exponential Latency & Token Waste:** Incurring LLM token overhead and latency for 8 agents repeatedly causes cascading replanning loops.
3. **Non-Convergence Risk:** Regenerating clean subgraphs risks introducing new, unrelated collisions in previously solved sectors.

---

## 2. Minimal Subgraph Invalidation & Preservation Invariant

The `SelectiveReplanningCoordinator` implements the **Minimal Subgraph Invalidation Principle**:

$$\text{Scope}_{\text{replan}} = \text{DirectlyConflictedIntents} \cup \text{DownstreamDependents}(\text{DirectlyConflictedIntents})$$

$$\text{PreservedIntents} = \text{TotalPlan} \setminus \text{Scope}_{\text{replan}}$$

### Properties Enforced:
1. **Preservation Invariant:** Every intent $I_k$ whose target resource and dependency tree does not intersect the conflict graph is marked `PRESERVED` and carried forward into the final plan without recomputation.
2. **Targeted Agent Invocation:** Only agents who authored intents in $\text{Scope}_{\text{replan}}$ are asked to replan.
3. **Deterministic Halting:** To prevent infinite ping-pong replanning loops, the coordinator enforces:
   - A maximum iteration bound (default: 5 iterations).
   - Conflict signature cycle detection: if the exact same set of conflicts recurs, replanning halts immediately and escalates to human review.

---

## 3. Algorithmic Workflow

```java
public ReplanningResult coordinate(
        Collection<ActionIntent> initialIntents,
        DigitalTwinSnapshot snapshot,
        BiFunction<Set<IntentId>, Integer, List<ActionIntent>> replanSupplier
)
```

1. **Conflict Detection:** Run `ConcurGuardConflictDetector` on active pool of candidate intents.
2. **Convergence Check:** If zero conflicts detected, return `ReplanningResult.successful(currentPool)`.
3. **Precedence Arbitration:** Run `ConcurGuardArbitrator` to obtain decisions for all intents.
4. **Subgraph Isolation:**
   - Compute `invalidatedIds` = intents receiving `REJECTED`, `REQUEST_REPLANNING`, or having unresolved conflicts.
   - Using `IntentDependencyGraph`, compute all transitive downstream dependents of `invalidatedIds` and add them to the invalidation set.
   - Isolate `preservedIds` = all other candidate intents.
5. **Cycle Detection Check:** Hash the set of conflict IDs. If observed in a previous iteration, break loop and return `ReplanningResult.escalated("Cyclic conflict loop detected")`.
6. **Iteration Bound Check:** If iteration reaches `maxIterations`, break loop and escalate to human decision-maker.
7. **Selective Generation:** Invoke `replanSupplier` passing only `invalidatedIds`.
8. **Pool Update:** Remove invalidated intents from pool, insert replanned proposals, keep preserved intents intact, and repeat from Step 1.
