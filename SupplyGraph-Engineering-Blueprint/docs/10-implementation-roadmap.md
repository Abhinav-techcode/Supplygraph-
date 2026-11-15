# 10 — Implementation Roadmap

## Phase 0 — Repository and decision baseline

Deliver:
- Inspect existing repository and branch state.
- Confirm no important user work will be overwritten.
- Add this blueprint and a concise README.
- Record architecture decisions (ADRs).
- Define coding conventions and branch/commit practices.
- Select compatible stable dependency versions from official sources.

Exit criteria:
- Product scope and non-goals reviewed.
- Java/framework compatibility recorded.
- No application code generated prematurely.

## Phase 1 — Build foundation

Deliver:
- Maven parent and minimum viable module structure.
- Java toolchain and formatting/checkstyle policy.
- Spring Boot application startup.
- Vaadin Flow landing page and navigation shell.
- PostgreSQL connection and Flyway migration baseline.
- Health/readiness endpoints.
- CI build and test workflow.

Exit criteria:
- Clean clone builds using documented commands.
- Application starts locally.
- Database migration runs on an empty database.
- CI runs tests successfully.

## Phase 2 — Core domain and digital twin

Deliver:
- Workspace and industry profile.
- Supplier, facility, material, product, BOM and inventory models.
- Supply relationship graph representation.
- State versioning and provenance.
- Initial seed dataset clearly labelled synthetic.
- Core dependency traversal tests.

## Phase 3 — Industry extension framework

Deliver:
- Industry module API and registration.
- EV battery pack first.
- Eight EV scenario definitions and acceptance tests.
- Add remaining three packs only after the extension contract is stable.

## Phase 4 — Disruption and impact analysis

Deliver:
- Incident lifecycle.
- Event ingestion abstraction.
- Impact analysis and dependency-path explanation.
- Scenario runner for synthetic/imported events.

## Phase 5 — Agent orchestration

Deliver:
- Coordinator and bounded specialized agent capabilities.
- Structured proposal schema.
- Allow-listed Java tools.
- Deterministic fake agent provider for tests.
- Real LLM provider adapter only after contracts and safety tests.

## Phase 6 — ConcurGuard

Deliver:
- Action intent registry.
- Resource normalization and claims.
- Quantity, capacity, temporal, semantic, dependency and stale-state conflict checks.
- Arbitration decisions.
- Selective replanning workflow.
- Adversarial and concurrency tests.

## Phase 7 — Optimization and simulation

Deliver:
- Initial optimization model and constraints.
- Deterministic simulation engine.
- Recovery-plan comparison.
- Reproducible experiment records.

## Phase 8 — Decision centre and remaining industry packs

Deliver:
- Disruption, agent, conflict, recovery and simulation views.
- Human approval.
- Pharma, semiconductor and automotive packs with scenario tests.

## Phase 9 — Hardening and research evaluation

Deliver:
- Security review.
- Performance and reliability tests.
- Baseline comparison experiments.
- Reproducible reports and limitations.
- Deployment and operations documentation.

## Delivery discipline

- Work on one phase and a small vertical slice at a time.
- Do not begin the next phase until exit criteria are met or explicitly waived.
- Each PR/commit should have a focused scope.
- Report files changed, commands run, test counts and known limitations.
- Never mark a phase complete based only on code generation.
