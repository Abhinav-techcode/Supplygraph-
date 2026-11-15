# SupplyGraph — Project Understanding & Vision

**Document ID:** `SG-ENG-REV-001`  
**Author:** Principal Software Architect & Agentic AI Research Engineer  
**Date:** November 2026  
**Status:** Architecture Review Baseline  

---

## 1. Executive Summary

**SupplyGraph** is an enterprise-grade, industry-configurable, software-only platform written in modern Java. It unifies **multi-tier supply network digital twin modeling**, **disruption detection with cascading impact analysis**, **collaborative multi-agent recovery intelligence**, **deterministic what-if simulation and optimization**, and **semantic, plan-level conflict coordination (ConcurGuard)** under a human-governed decision support framework.

SupplyGraph is designed to solve one of the most critical structural flaws in modern supply chains: **uncoordinated, fragmented decision-making during systemic disruptions**.

---

## 2. The Problem SupplyGraph Solves

### 2.1 The Reality of Global Supply Chain Fragility
Enterprise supply networks across high-complexity sectors—such as Electric Vehicle (EV) battery manufacturing, pharmaceuticals, semiconductor fabrication, and precision automotive components—operate as deeply interconnected, multi-tier directed graphs. In these networks:
- A disruption at a Tier-3 chemical precursor supplier cascades through Tier-2 subcomponents and Tier-1 modules, eventually halting production lines at Tier-0 assembly plants.
- Operational silos (Procurement, Logistics, Inventory, Production, Quality) react independently using disjoint tools (ERP, WMS, TMS, MES).
- Functional managers optimize local objectives:
  - *Procurement* rushes to order alternative parts without checking incoming dock capacity or storage compliance.
  - *Logistics* reroutes shipments through air freight without verifying if the destination warehouse has available quarantine space or customs clearance.
  - *Production* reschedules assembly lines without knowing that shared critical raw materials have already been reallocated to another product line.

### 2.2 Why Naive Generative AI Makes the Problem Worse
Recent attempts to apply Large Language Models (LLMs) or autonomous multi-agent systems to supply chains suffer from severe hazards:
1. **Hallucination of Feasibility:** LLMs generate fluent, plausible-sounding recovery plans that violate physical laws, mass-balance conservation, bill-of-materials (BOM) constraints, or temporal precedence.
2. **Authoritative Arithmetic Failure:** LLMs cannot perform reliable arithmetic on multi-million dollar inventory positions, lead-time offsets, or capacity limits.
3. **Competing Agent Chaos:** Autonomous agents acting on behalf of different departments issue contradictory commands (e.g., both claiming the same remaining batch of lithium carbonate or the same cleanroom time-slot).
4. **Database Locking Is Not Plan Coordination:** Ordinary ACID transactions or database row locks prevent concurrent writes to the same row at milliseconds scale, but cannot understand that two plans created 2 hours apart to book the same truck or consume the same batch 5 days in the future are mutually exclusive.

### 2.3 The SupplyGraph Solution
SupplyGraph replaces fragmented guesswork with a **scientifically grounded, deterministic, and human-in-the-loop recovery platform**:
- **Digital Twin:** A single, versioned graph model of facilities, suppliers, inventory lots, transport routes, and production stages.
- **Cascading Impact Engine:** Deterministic graph traversal that traces exactly which downstream customer orders and assembly lines are endangered by an upstream disruption.
- **Specialized AI Agents:** Bounded, domain-specific agents that investigate alternatives and propose structured recovery intents without mutating authoritative records.
- **ConcurGuard:** A plan-level conflict coordination engine that normalizes intents, detects multi-dimensional resource, temporal, and semantic conflicts, and executes **selective replanning** to preserve valid actions while repairing conflicting ones.
- **Deterministic Optimizer & Simulator:** Mathematical programming (Google OR-Tools) and discrete-event simulation to verify feasibility and calculate quantifiable KPIs (cost, delay, service level).
- **Human Approval Boundary:** All consequential recovery actions require explicit human review, full evidence tracing, and recorded rationale.

```
+-----------------------------------------------------------------------------------+
|                            HUMAN DECISION CENTRE (UI)                             |
|              Approval Workflows | Evidence Inspection | Trade-off Analysis        |
+-----------------------------------------------------------------------------------+
                                          ▲
                                          │ Human Sign-off
+-----------------------------------------------------------------------------------+
|                         RECOVERY VALIDATION & SIMULATION                          |
|         Deterministic Discrete-Event Simulation | OR-Tools Optimization           |
+-----------------------------------------------------------------------------------+
                                          ▲
                                          │ Validated Feasible Plans
+-----------------------------------------------------------------------------------+
|                         CONCURGUARD COORDINATION ENGINE                           |
|        Intent Registry | Semantic Conflict Detection | Selective Replanning       |
+-----------------------------------------------------------------------------------+
                                          ▲
                                          │ Structured Action Proposals (Read-Only)
+-----------------------------------------------------------------------------------+
|                         AGENTIC AI INVESTIGATION CLUSTER                          |
|  Risk Intel | Inventory | Procurement | Logistics | Production | Quality | Planner    |
+-----------------------------------------------------------------------------------+
                                          ▲
                                          │ Context & State Snapshots
+-----------------------------------------------------------------------------------+
|                    SUPPLY CHAIN DIGITAL TWIN & IMPACT ENGINE                      |
|       Multi-Tier Network Graph | Versioned State | Cascading Dependency Tracing   |
+-----------------------------------------------------------------------------------+
                                          ▲
                                          │ Operational Events
+-----------------------------------------------------------------------------------+
|                            POSTGRESQL TRANSACTIONAL TRUTH                         |
+-----------------------------------------------------------------------------------+
```

---

## 3. Product Anatomy & Core Capabilities

### 3.1 Multi-Tier Supply Network Digital Twin
The digital twin maintains a high-fidelity graph representation of the entire supply ecosystem:
- **Nodes:** Organizations, Suppliers, Facilities (Factories, Warehouses, Distribution Centers), Inventory Positions, Lots/Batches, Products, Materials, Components, and Customer Orders.
- **Edges:** Supply Relationships, Transport Routes, BOM Hierarchies, and Sequential Production Stages.
- **Temporal State & Versioning:** Every state mutation produces an immutable, monotonically increasing state version (`stateVersion`). All planning, agent queries, and simulations execute against an explicit state version, eliminating race conditions and phantom reads.

### 3.2 Industry Intelligence Packs (Extensible Architecture)
The platform is industry-agnostic at its core, but configured through pluggable **Industry Intelligence Packs**. Four initial reference packs are defined:
1. **EV Battery Manufacturing:** Cell chemistries (NMC, LFP), critical precursors (Lithium, Nickel, Cobalt), cleanroom dry-rooms, aging/formation stages, thermal runaway testing, battery recycling streams.
2. **Pharmaceutical Supply Chain:** Active Pharmaceutical Ingredients (APIs), cold-chain integrity (-80°C to +8°C), lot/batch genealogy, shelf-life/expiry windows, regulatory holds, Good Manufacturing Practice (GMP) quarantine rules.
3. **Semiconductor Manufacturing:** Silicon wafers (200mm/300mm), photolithography steppers, extreme purity specialty chemicals (HF, photoresists), fab cleanroom classes, yield curves, front-end fab vs. back-end packaging and test bottlenecks.
4. **Automotive Component Manufacturing:** Just-in-Time (JIT) / Just-in-Sequence (JIS) line delivery, Tier-1 through Tier-3 supply tiers, assembly line takt times, specialized stamping dies, and vehicle programme dependencies.

Each industry pack contributes:
- Domain entity extensions and specific terminology.
- 8 initial disruption scenario definitions (32 total baseline scenarios).
- Industry-specific hard constraints and validation policies.
- Domain KPIs (e.g., line-stop risk, batch spoilage, yield loss, compliance penalty).
- Tool specifications and acceptance tests.

### 3.3 Disruption Lifecycle & Impact Analysis
- **Lifecycle:** `REPORTED` -> `ANALYZING` -> `INVESTIGATING` -> `PLAN_GENERATED` -> `CONFLICT_CHECKED` -> `SIMULATED` -> `PENDING_APPROVAL` -> `APPROVED` / `REJECTED` -> `EXECUTED` -> `RESOLVED`.
- **Event Types:** Supply shortages, price surges, transit delays, equipment breakdowns, quality defect spikes, cold-chain / cleanroom excursions, sudden demand spikes, and geopolitical / trade restrictions.
- **Cascading Analysis:** Upstream-to-downstream graph traversal calculating time-to-impact ($T_{impact}$), total inventory runway ($R_{inventory}$), affected order values, and critical dependency bottlenecks.

### 3.4 Bounded Agentic AI Investigation
SupplyGraph coordinates 8 specialized agents via a central Coordinator:
1. **Coordinator Agent:** Manages execution phases, assigns sub-tasks, enforces investigation timeouts, and synthesizes findings.
2. **Risk Intelligence Agent:** Evaluates disruption evidence, external context, and geopolitical/environmental threat levels.
3. **Inventory Agent:** Analyzes available vs. reserved stock, safety stock buffers, and batch expiry.
4. **Procurement Agent:** Identifies qualified secondary suppliers, evaluates pricing, lead times, and contractual terms.
5. **Logistics Agent:** Explores alternate transportation modes (air, sea, rail, expedited road), multi-leg routing, and carrier capacity.
6. **Production Agent:** Identifies alternate manufacturing lines, shifts, and capacity reallocation.
7. **Quality Agent:** Evaluates compliance with industry standards, quarantine protocols, and excursion tolerances.
8. **Recovery Planning Agent:** Combines domain proposals into cohesive end-to-end candidate recovery strategies.

**Absolute Safety Contract:** Agents are strictly investigation and proposal engines. They invoke only allow-listed, read-only Java tools. They have **zero direct write access** to authoritative database tables.

### 3.5 ConcurGuard: Semantic Conflict Detection & Selective Replanning
ConcurGuard is the core research and engineering differentiator. It sits between agent proposal generation and plan execution:
- **Normalization:** Converts diverse agent proposals into standardized **Action Intents** with formal **Resource Claims**.
- **Conflict Dimensions:**
  - *Quantity Conflicts:* Combined allocations exceed physically available inventory.
  - *Capacity Conflicts:* Combined throughput exceeds daily/weekly machine or dock limits.
  - *Temporal Overlaps:* Conflicting claims on the same resource during intersecting time windows $[t_{start}, t_{end})$.
  - *Semantic Overlaps:* Differently named actions that secretly consume the same underlying asset (e.g., "Air Expedite" and "Warehouse Transfer" both booking the same staging dock bay).
  - *Dependency Preconditions:* Action $B$ requires completion of Action $A$, but $A$ is delayed or rejected.
  - *Stale-State Conflicts:* The digital twin state changed while the agent was generating its proposal.
  - *Industry Policy Conflicts:* Violations of cold-chain, GMP, or qualified supplier rules.
- **Selective Replanning:** When an intent is invalidated, ConcurGuard does not discard the entire multi-action plan. It computes the minimal affected dependency subgraph, preserves all unaffected valid intents, triggers targeted replanning only for the invalid actions, and re-arbitrates.

### 3.6 Deterministic Optimization & Digital Twin Simulation
- **Mathematical Optimization (Google OR-Tools):** Solves Mixed-Integer Linear Programming (MILP) and Constraint Programming (CP-SAT) models for multi-commodity network flow, optimal sourcing allocation, and production rescheduling under hard constraints.
- **Discrete-Event Simulation:** A deterministic Java simulation clock models step-by-step physical reality (shipment dispatch, transit, arrival, batch processing, line stops) across three comparative branches:
  1. *Baseline (No Disruption)*
  2. *Disrupted (Unmitigated Impact)*
  3. *Recovery Plan (Mitigated Outcome)*
- **Objective Metrics:** Explicit multi-criteria scoring: Cost of Recovery, Total Order Delay, Service Level (OTIF), and Preserved Value.

### 3.7 Human Governance & Decision Centre
- Human operators interact through a **Vaadin Flow** Java-authored UI.
- Dashboards display digital twin network graphs, active disruptions, cascading impact trees, side-by-side recovery scenarios, conflict logs, and simulation comparisons.
- Operators have full authority to approve, modify, delay, or reject recovery actions. Every decision is cryptographically auditable with complete provenance.

---

## 4. Strict Engineering Scope & Non-Goals

| Area | In-Scope (Strict Commitment) | Explicit Non-Goal (Forbidden) |
|---|---|---|
| **Hardware / IoT** | Software-based event ingestion, synthetic telemetry generation, CSV/JSON data imports | ESP32, physical microcontrollers, hardware sensors, MQTT/Mosquitto brokers, PLC integration |
| **Frontend Stack** | Vaadin Flow (pure Java UI components, server-driven views, responsive layout) | React, Angular, Vue, Node.js, separate JavaScript SPA |
| **Backend Stack** | Java 21 LTS / Java 25, Spring Boot 3.4.x, Maven multi-module modular monolith | Python backend services, Scala, Go, microservice sprawl |
| **AI Integration** | Spring AI, typed Java tool interfaces, deterministic fake provider for tests | Autonomous agents mutating DB, LLM arithmetic, unbounded loops |
| **Operations** | Decision support, scenario simulation, human approval workflows | Autonomous placement of live purchase orders, ERP direct overwrite, automated factory control |
| **Infrastructure** | PostgreSQL (authoritative store), Flyway, Testcontainers, Docker Compose | Mandatory Kafka or Neo4j before concrete load/query justification |

