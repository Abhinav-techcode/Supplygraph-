# SupplyGraph — Open Decisions & Stakeholder Clarifications

**Document ID:** `SG-ENG-REV-010`  
**Author:** Principal Software Architect & Technical Program Manager  
**Date:** November 2026  
**Status:** Open Decisions Baseline  

---

## 1. Purpose

This document catalogs critical architectural, operational, and domain decisions that require explicit stakeholder alignment before or during implementation.

To maintain engineering discipline, each item explicitly distinguishes between:
- **Documented Requirement:** Mandated by the blueprint.
- **Architectural Recommendation:** Proposed by the engineering architecture review.
- **Underlying Assumption:** Hypothesized based on industry best practices.
- **Open Decision:** Requires user/stakeholder approval.

---

## 2. Decision Register

### DEC-001: Java Language Baseline — Java 21 LTS vs. Java 25 LTS
- **Documented Requirement:** Blueprint specifies Java 25 LTS as intended baseline, subject to verified framework compatibility across the release train.
- **Architectural Recommendation:** Set Maven bytecode compiler release to **Java 21 LTS (`<java.version>21</java.version>`)**, while allowing builds to run on JDK 21 through JDK 26.
- **Technical Justification:** Java 21 LTS is currently the universal production standard supported natively across Spring Boot 3.4, Vaadin Flow 24.5, Spring AI, Testcontainers, and Google OR-Tools JNI native bindings. Targeting Java 21 bytecode prevents bytecode manipulation conflicts in ByteBuddy/Hibernate and JNI memory layout crashes in OR-Tools, while retaining virtual threads (`Loom`), records, and pattern matching.
- **Status:** **PENDING STAKEHOLDER APPROVAL.**

---

### DEC-002: Infrastructure Scope — PostgreSQL Alone vs. Immediate Neo4j & Kafka
- **Documented Requirement:** PostgreSQL is the transactional source of truth. Neo4j and Kafka are proposed candidate technologies, but must not be added prematurely without justified scale, consistency models, and operational cost analysis.
- **Architectural Recommendation:** **Adopt PostgreSQL 16+ as the sole database and use in-process Spring events for the initial milestones (Phases 1–8).** Defer Neo4j and Kafka.
- **Technical Justification:**
  - *Graph Queries:* PostgreSQL Recursive CTEs traverse 10 tiers of supply networks in < 10ms for datasets under 100,000 nodes. Introducing Neo4j creates a complex distributed dual-write synchronization problem.
  - *Messaging:* In-process event dispatching eliminates Kafka broker infrastructure, ZooKeeper/KRaft cluster management, and network partition failure modes.
  - *Clean Ports:* Domain repositories will use generic interfaces (`SupplyGraphRepository`, `EventPublisherPort`) allowing Neo4j and Kafka adapters to be introduced in Phase 9 if scale metrics dictate.
- **Status:** **PENDING STAKEHOLDER APPROVAL.**

---

### DEC-003: Inclusion of Maven Wrapper (`./mvnw`)
- **Documented Requirement:** Maven multi-module build.
- **Architectural Recommendation:** Include the official Maven Wrapper (`./mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) in the repository root.
- **Technical Justification:** The host environment currently does not have `mvn` in its `$PATH`. The Maven Wrapper guarantees reproducible, hermetic builds across all developer workstations and CI runners without requiring manual global package installations.
- **Status:** **RECOMMENDED — PROCEED IN PHASE 1.**

---

### DEC-004: Interactive Graph Visualization in Vaadin Flow
- **Documented Requirement:** Vaadin Flow frontend for digital twin and network health visualization.
- **Architectural Recommendation:** Implement a lightweight client-side Web Component wrapper (integrating **Cytoscape.js** or **Vis-Network**) embedded inside a Vaadin Flow custom Java component.
- **Technical Justification:** Pure server-side DOM rendering of 500+ nodes and edges causes high WebSocket payload churn and sluggish browser repainting. A client-side canvas/WebGL component receiving compact JSON node/edge payloads ensures 60 FPS interactive zoom, pan, and clustering.
- **Status:** **OPEN ARCHITECTURAL ALIGNMENT.**

---

### DEC-005: Industry Pack Sequencing Order
- **Documented Requirement:** Shared platform supporting 4 initial industry packs: EV Battery, Pharma, Semiconductor, Automotive.
- **Architectural Recommendation:** Sequential delivery:
  1. **EV Battery Manufacturing:** First vertical slice in Phase 3 (establishes the SPI, chemical BOM, dry-room capacity constraints).
  2. **Pharmaceutical Supply Chain:** Phase 8 (adds strict cold-chain excursions, batch traceability, and GMP regulatory holds).
  3. **Semiconductor Manufacturing:** Phase 8 (adds wafer lot yield curves, fab tool bottlenecks, and extreme purity chemical constraints).
  4. **Automotive Component Manufacturing:** Phase 8 (adds multi-tier Tier-1 to Tier-3 JIT/JIS delivery, line-stop takt times).
- **Technical Justification:** Hardening the SPI extension contract against one complex industry (EV Battery) before fanning out to the other three prevents parallel refactoring churn across four modules.
- **Status:** **RECOMMENDED SEQUENCE.**

---

### DEC-006: Spring AI Provider Selection for Local Development
- **Documented Requirement:** Spring AI abstraction, typed tools, and prompt injection defense.
- **Architectural Recommendation:** Provide a **three-tier provider strategy**:
  1. `DeterministicFakeAiClient`: Default for all unit, integration, and CI tests (zero API cost, 100% reproducible).
  2. `Ollama / Local LLM`: Supported for offline local developer evaluation (e.g., Llama 3 / Mistral).
  3. `Gemini / OpenAI API`: Configurable via environment variables for live demonstrations.
- **Status:** **RECOMMENDED PATTERN.**

