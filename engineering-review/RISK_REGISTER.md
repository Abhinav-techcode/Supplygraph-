# SupplyGraph — Comprehensive Risk Register & Mitigation Strategy

**Document ID:** `SG-ENG-REV-008`  
**Author:** Principal Software Architect & Quality Assurance Lead  
**Date:** November 2026  
**Status:** Risk Assessment Baseline  

---

## 1. Risk Classification Matrix

Risks are categorized by severity:
- **CRITICAL:** Threatens architectural viability, data integrity, or core system reliability.
- **HIGH:** Threatens project delivery timeline, performance, or security boundaries.
- **MEDIUM:** Requires explicit engineering trade-offs, testing, or domain clarification.
- **LOW:** Manageable through standard conventions and refactoring.

---

## 2. Identified Risks & Mitigation Plans

### 2.1 Technical & Architectural Risks

#### RISK-001: Premature Infrastructure Overhead (Kafka & Neo4j)
- **Classification:** **HIGH**
- **Description:** Introducing Kafka (distributed messaging) and Neo4j (graph database) into the initial release creates dual-write distributed transaction complexity, synchronization drift, and high Docker resource consumption.
- **Why it matters:** Multi-tier supply networks at MVP scale (10,000 nodes) fit completely into modern server RAM. A dual-store setup requires 2-Phase Commits or complex CDC outbox patterns to prevent PostgreSQL and Neo4j from drifting out of sync.
- **Consequence if ignored:** Significant development time wasted managing Kafka consumer offsets, Zookeeper/KRaft, and Neo4j sync bugs rather than building core domain value.
- **Recommended Resolution:** Use PostgreSQL 16+ as the single authoritative store. Implement graph traversal using recursive Common Table Expressions (CTEs) and an in-memory graph cache (`JGraphT`). Wrap event publishing behind an `EventPublisherPort` so Kafka can be added later with zero domain changes.

#### RISK-002: Java 25 LTS Framework Compatibility & JNI Native Stability
- **Classification:** **HIGH**
- **Description:** The blueprint specifies Java 25 LTS as the intended baseline. In late 2026, while JDK 25 runs, certain third-party libraries (Google OR-Tools JNI native libraries, bytecode instrumentation agents like ByteBuddy/CGLIB) require caution.
- **Why it matters:** JNI crashes in Google OR-Tools or bytecode generation failures in Hibernate can halt application startup or cause unrecoverable native segmentation faults.
- **Consequence if ignored:** Intermittent build failures, lack of IDE tooling support, and runtime crashes during mathematical solver execution.
- **Recommended Resolution:** Pin Maven compiler release to Java 21 LTS bytecode target (`<java.version>21</java.version>`) while allowing compilation and execution on JDK 21 through JDK 26. This guarantees 100% binary stability with OR-Tools while retaining modern Java features.

#### RISK-003: Non-Deterministic LLM Arithmetic & Hallucinated Feasibility
- **Classification:** **CRITICAL**
- **Description:** Generative AI agents might attempt to perform arithmetic (e.g., calculating available inventory, lead time dates, or unit conversions) or invent plausible-sounding suppliers and prices.
- **Why it matters:** LLMs are statistical text generators, not mathematical engines. A single hallucinated quantity (e.g., allocating 1,000 kg when only 100 kg exists) can halt a factory production line.
- **Consequence if ignored:** Infeasible recovery plans generated, loss of user trust, and severe business disruption.
- **Recommended Resolution:** Architectural hard boundary: Agents are strictly read-only investigators. Agents invoke typed, allow-listed Java tools to fetch real data. All arithmetic, cost calculations, capacity validations, and constraints are executed exclusively by deterministic Java domain code and Google OR-Tools.

#### RISK-004: ConcurGuard Combinatorial Explosion in Multi-Action Replanning
- **Classification:** **HIGH**
- **Description:** As the number of simultaneous disruptions and proposed action intents grows, checking multi-dimensional temporal overlap and dependency paths could become computationally expensive ($O(N^2)$ or higher).
- **Why it matters:** Real-time disruption recovery requires interactive UI response times (< 2 seconds) for control-tower operators.
- **Consequence if ignored:** UI freezes, thread exhaustion, or solver timeouts during complex cascading crisis events.
- **Recommended Resolution:** Index resource claims by `(ResourceId, TimeBucket)`. Use interval trees for $O(\log N)$ temporal overlap queries. Bound the replanning search depth and partition the dependency graph by independent facilities.

#### RISK-005: Large Graph Visualization Performance in Vaadin Flow
- **Classification:** **MEDIUM**
- **Description:** Vaadin Flow renders UI server-side over WebSocket. Pushing large, deeply nested graph networks (hundreds of nodes and edges) via server-side DOM trees can cause high memory consumption and browser rendering sluggishness.
- **Why it matters:** Control-tower analysts need fluid, responsive network exploration.
- **Consequence if ignored:** Sluggish UI, high server session memory overhead, and degraded user experience.
- **Recommended Resolution:** Implement a dedicated, lightweight client-side graph visualizer component (e.g., Cytoscape.js or Vis.js wrapper) with server-side pagination, level-of-detail (LOD) aggregation, and viewport-constrained node rendering.

---

### 2.2 Domain & Compliance Risks

#### RISK-006: Regulatory & Quality Rule Oversimplification (Pharma & EV Batteries)
- **Classification:** **HIGH**
- **Description:** Real-world pharmaceutical (FDA 21 CFR Part 11, EU GMP Annex 1) and EV battery (UN 38.3, thermal stability) standards have complex statutory requirements. The initial software pack cannot capture all legal intricacies.
- **Why it matters:** Claiming that software automatically certifies a drug or battery lot as safe after a temperature excursion creates legal liability and safety hazards.
- **Consequence if ignored:** Regulatory non-compliance and hazardous decision-making.
- **Recommended Resolution:** Strict compliance disclaimer: Software definitions are engineering abstractions. The system enforces **automatic quarantine** on excursions; it **never automatically declares an item released or safe**. Release requires documented human specialist sign-off.

#### RISK-007: Synthetic Data Contamination & False Benchmarking
- **Classification:** **MEDIUM**
- **Description:** Mixing synthetic demonstration data with imported operational data could mislead operators or corrupt research evaluations.
- **Why it matters:** Operators must know with absolute certainty whether a supplier disruption is a live operational event or a synthetic simulation drill.
- **Consequence if ignored:** Mistaken operational panic or compromised experiment credibility.
- **Recommended Resolution:** Mandatory, non-nullable `DataProvenance` field on all entities. UI displays prominent visual badges: `[SYNTHETIC DEMO]`, `[IMPORTED]`, `[EXTERNAL INTEGRATION]`.

---

### 2.3 Operational & Tooling Risks

#### RISK-008: Host Environment Tooling Discrepancy (Missing Global Maven)
- **Classification:** **LOW**
- **Description:** The host environment lacks a global `mvn` command in `$PATH`.
- **Why it matters:** New developers or CI environments without global Maven cannot execute builds.
- **Consequence if ignored:** Manual build friction and setup failures.
- **Recommended Resolution:** Bundle the standard **Maven Wrapper (`./mvnw`)** in the repository root in Phase 1, ensuring zero-dependency builds on any machine with a JDK.

