# SupplyGraph — Engineering Blueprint

**Product:** SupplyGraph  
**Positioning:** Industry-configurable, Java-first agentic supply-chain intelligence, digital-twin simulation and conflict-aware recovery platform.  
**Status:** Planning baseline — not an implementation specification signed off against real industry operators.

## Purpose

This blueprint is the source of truth for planning and implementing SupplyGraph professionally. It is designed to be placed in the root of the GitHub repository and used by Antigravity as project context.

## Product baseline

- Software-only product. **No physical IoT devices, ESP32, sensors, MQTT brokers or hardware integration.**
- Java-first application from application frontend through backend. Use Vaadin Flow for Java-authored UI and Spring Boot for backend services. A browser still uses browser technologies internally; the project will not maintain a separate React/Node frontend.
- One shared platform supporting four initial industry packs:
  1. EV battery manufacturing
  2. Pharmaceutical supply chain
  3. Semiconductor manufacturing
  4. Automotive component manufacturing
- Eight initial disruption scenarios per industry (32 initial scenario definitions).
- Industry packs are extensible; do not hardcode a limit of four industries or 32 scenarios.
- Core research/engineering focus: ConcurGuard — conflict-aware coordination and selective replanning of competing agent-proposed recovery actions.
- Human approval is required for consequential actions. Initial release is decision-support and simulation, not autonomous control of real industrial operations.

## Documents

| File | Purpose |
|---|---|
| `docs/01-product-vision.md` | Product goals, users, scope and non-goals |
| `docs/02-functional-requirements.md` | Platform-wide functional requirements |
| `docs/03-industry-packs-and-scenarios.md` | Four industries and 32 scenario definitions |
| `docs/04-architecture.md` | Target architecture and component responsibilities |
| `docs/05-java-technology-stack.md` | Java stack and dependency decisions |
| `docs/06-domain-model.md` | Core domain concepts and invariants |
| `docs/07-agentic-ai-and-concurguard.md` | Agent contracts, intent registry, conflict handling |
| `docs/08-simulation-and-optimization.md` | Simulation, optimization and evaluation |
| `docs/09-quality-security-and-operations.md` | Clean code, testing, security and operations |
| `docs/10-implementation-roadmap.md` | Incremental delivery plan and exit criteria |
| `ANTIGRAVITY_MASTER_PROMPT.md` | Master instruction for Antigravity |

## Important engineering rules

1. Treat these documents as a proposed baseline. Identify contradictions, risks and missing decisions before implementation.
2. Do not invent real industry data, supplier relationships, regulatory rules, sensor readings or benchmark results. Label synthetic data clearly.
3. Do not claim research novelty until a literature and product prior-art review has been performed.
4. Do not introduce Python, Node.js, React, Scala or a separate frontend service into the application architecture.
5. Do not add microservices merely for fashion. Start with a Maven multi-module modular monolith.
6. Do not add Kafka, Neo4j or other infrastructure until the design explains the concrete use case and operational cost. They are target technologies, not permission to overengineer.
7. All business rules and feasibility checks must be deterministic and testable. LLMs may interpret, investigate and propose; they are not the source of truth for quantities, costs, capacity or feasibility.
8. Keep a human approval boundary for consequential recommendations and any future external execution.
9. Work in small, reviewable changes. Run tests and report actual results; never claim a test passed if it was not run.
10. Do not overwrite or delete existing repository content without inspecting it and explaining the intended change.

## Suggested first milestone

Create the repository documentation and Maven parent structure only after reviewing the repository state. Do not generate the entire application in the first pass.
