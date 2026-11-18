# SupplyGraph — Local Demo Environment & Repository Audit
## Pre-Deployment Verification & System Inspection

**Date:** November 18, 2026  
**Auditor:** Principal Enterprise Software Architect & QA Verification Lead  
**Repository:** `/Users/abhinavgupta/Desktop/Supplygraph`  
**Git Branch:** `phase-9-10-integration-runtime-and-governed-execution`  
**HEAD Commit:** `9dfc00c` ("Implement Phase 9 & 10: enterprise integration runtime and governed closed-loop execution")  

---

## 1. Git Safety & Working Tree Status

| Parameter | Observed Value | Safety Verification |
|---|---|---|
| **Current Branch** | `phase-9-10-integration-runtime-and-governed-execution` | Non-main working branch; origin/main at `12ed537` |
| **Tracked Changes** | Modified domain/persistence/service files across Phases 7-15 | All preserved; no destructive git commands allowed |
| **Untracked Files** | New Phase 11-15 domain, application, persistence, and web source files | Cleanly isolated in standard Maven source trees |
| **Git Safety Rules** | Strict prohibition on `git reset --hard`, `git clean -fd`, `git restore` | Zero risk of user data loss |

---

## 2. Local Environment Inspection

| Subsystem | Requirement | Actual Local State | Compliance |
|---|---|---|---|
| **Operating System** | macOS 14+ (Darwin 24+) | Darwin 26.6.2 (macOS 15.x / 16.x) aarch64 (Apple Silicon) | COMPLIANT |
| **Java Platform** | Java 21 LTS | OpenJDK 21.0.13.0.3 Homebrew (`/opt/homebrew/opt/openjdk@21`) | COMPLIANT |
| **Maven Wrapper** | Maven 3.9+ | Apache Maven 3.9.9 via bundled `./mvnw` | COMPLIANT |
| **PostgreSQL** | PostgreSQL 16+ | PostgreSQL 16.16 Alpine in Docker container `supplygraph-postgres` on port 5433 | COMPLIANT |
| **Docker / Colima** | Docker Engine 24+ | Colima socket `unix:///Users/abhinavgupta/.colima/default/docker.sock` | COMPLIANT |
| **OR-Tools Native** | Google OR-Tools 9.11+ | Google OR-Tools 9.11.4210 native macOS ARM64 binaries loaded | COMPLIANT |
| **Flyway Migrations** | v1 through v15 | 15 SQL migration scripts present in `db/migration/` | COMPLIANT |
| **AI Provider** | Bounded & Deterministic | Spring AI configured with `FakeAiAgentGatewayAdapter` (local reproducible offline operation) | COMPLIANT |
| **Application Port** | Port 8080 | Port 8080 unallocated and available | COMPLIANT |

---

## 3. Module & Architectural Boundaries Audit

The repository contains 14 Maven reactor modules:

1. `supplygraph` (Parent POM): Dependency management, plugin configurations, Java 21 release target.
2. `supplygraph-core-domain`: Canonical entities, value objects, double-entry inventory ledger, BOM DAG. 100% pure Java 21 with zero external framework dependencies.
3. `supplygraph-industry-api`: Pluggable SPI contracts (`IndustryPackExtension`, `IndustryIdentifier`, `IndustryPolicy`).
4. `supplygraph-concurguard`: Algorithmic conflict detection engine arbitrating 7 formal classes of recovery proposal conflicts.
5. `supplygraph-simulation-engine`: Discrete event simulation clock, Monte Carlo risk engine, and Google OR-Tools MIP solver integration.
6. `supplygraph-core-application`: Use cases, interactors, ports, and domain services.
7. `supplygraph-industry-ev`: EV Battery Manufacturing pack (8 scenarios).
8. `supplygraph-industry-pharma`: Pharmaceutical Supply Chain pack (8 scenarios, cold-chain).
9. `supplygraph-industry-semiconductor`: Semiconductor Manufacturing pack (8 scenarios, cleanroom).
10. `supplygraph-industry-automotive`: Automotive Component Manufacturing pack (8 scenarios, JIT/JIS).
11. `supplygraph-adapter-persistence`: PostgreSQL 16+ persistence adapters, Spring Data JPA, Flyway v1-v15.
12. `supplygraph-adapter-ai`: Spring AI adapter with bounded schema enforcement and deterministic offline fakes.
13. `supplygraph-adapter-integration`: Integration runtime, bidirectional connectors, outbox/inbox, SSRF-safe REST client.
14. `supplygraph-web`: Vaadin Flow 24.5.4 Java-authored UI, Spring Boot 3.4.1 composition root, Actuator endpoints.

---

## 4. Verification Strategy

The following phases will be tested directly against the running application on `http://localhost:8080`:
- **Phase 1–2:** Digital Twin, Directed Graph, BOM Explosion, Double-Entry Ledger.
- **Phase 3–4:** AI Agents, ConcurGuard 7-Class Conflict Matrix, DES Clock, Monte Carlo P10/P50/P90, OR-Tools MIP Solver.
- **Phase 5–6:** Transactional Outbox, Event Ingestion, RBAC (9 roles), SHA-256 Tamper-Evident Audit Log.
- **Phase 7–8:** 4 Pluggable Industry Packs, Pareto Frontiers, Explainable AI Attribution, CSV/JSON Ingestion.
- **Phase 9–10:** Connector Runtime, HMAC Webhooks, Governed Task Graph Execution, SoD Approvals.
- **Phase 11–13:** Multi-Echelon Planning (MEIO), Scope 1-3 Carbon Accounting, Traceability DAG, Supplier Portal.
- **Phase 14–15:** Enterprise Design System, 9 Persona Cockpits, Unified Command Centre, Tenant Quotas, Policy Versioning & 1-Click Rollback, API Key Vault.

