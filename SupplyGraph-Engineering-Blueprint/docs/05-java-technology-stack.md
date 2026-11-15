# 05 — Java Technology Stack

## Selected direction

Java-first application, Vaadin Flow frontend and Spring Boot backend. Use Java 25 LTS as the intended language baseline, subject to compatibility verification across the selected framework release train.

| Concern | Preferred technology | Decision notes |
|---|---|---|
| Language/runtime | Java 25 LTS | Verify framework and build-tool support before pinning versions. |
| Build | Maven | Parent POM and dependency management. |
| Backend | Spring Boot | Modular monolith, application composition and web endpoints. |
| Frontend | Vaadin Flow | UI views and event handlers authored in Java. |
| AI integration | Spring AI | LLM abstraction, tool calling and structured output; one framework initially. |
| Relational persistence | PostgreSQL | Transactional source of truth. |
| ORM | Spring Data JPA / Hibernate | Persistence adapter, not domain model leakage. |
| Graph | Neo4j Java Driver | Add only if graph query requirements justify operational cost. |
| Messaging | Spring Kafka / Kafka client | Add when asynchronous integration and event throughput justify it. |
| Optimization | Google OR-Tools Java | Constraint programming/optimization; model feasibility explicitly. |
| Simulation | Custom Java engine | Deterministic, versioned and testable. |
| Security | Spring Security | Authentication and server-side authorization. |
| Migrations | Flyway | Versioned database migrations. |
| Testing | JUnit 5, Mockito | Unit and application tests. |
| Integration testing | Testcontainers | Real PostgreSQL and, when needed, Neo4j/Kafka containers. |
| Observability | Micrometer, structured logging | Metrics, traces/log correlation and health. |
| Packaging | Docker | Reproducible local and deployment environment. |

## Version policy

Do not blindly use `latest` or copy version numbers from old tutorials. Before generating the POM:
1. Check current stable compatible releases from official documentation.
2. Verify Java 25 support for Spring Boot, Vaadin and Spring AI.
3. Select a compatible Spring Boot/Vaadin/Spring AI combination.
4. Pin versions through parent dependency management.
5. Record the versions and verification date in `docs/architecture/technology-decisions.md`.
6. Avoid milestone, snapshot or release-candidate dependencies unless explicitly approved.

If a required framework does not yet support Java 25 in the chosen stable release, report the evidence and propose the smallest compatible Java LTS alternative before changing the baseline.

## Java-only clarification

The application source and maintained UI/backend modules should be Java. Vaadin renders a web UI in browsers using its framework runtime; this does not mean browser internals are Java. No separately maintained React, Node.js or Python application is planned.

## Avoid premature infrastructure

Neo4j and Kafka are target options, not automatic requirements. Build interfaces/adapters and document the decision. A local PostgreSQL plus in-process event abstraction may be the correct first milestone. Add infrastructure only with a concrete requirement, tests and documented operations cost.
