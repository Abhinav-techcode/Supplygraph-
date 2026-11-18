# Phase 9 & 10 — Architecture and Gap Analysis

Status: **in progress**. Verified against the repository at commit `12ed537` (+ uncommitted Phase 7/8 working-tree changes, left untouched).

## 1. Verified baseline
- Toolchain: Java 21 (Temurin 21.0.13 in `~/.jdks/jdk21`; the machine default JDK 26 cannot run Mockito 5/ByteBuddy from Spring Boot 3.4.1, so 12 tests error on JDK 26).
- `JAVA_HOME=~/.jdks/jdk21/Contents/Home ./mvnw -o clean verify` → BUILD SUCCESS, **194 tests, 0 failures** (matches the reported figure). The persistence module takes ~8.5 min because its Testcontainers tests start PostgreSQL.
- 12 reactor projects + parent (13), Flyway V1–V7.

## 2. Existing capabilities (reuse, do not duplicate)
| Area | Existing | Location |
|---|---|---|
| CSV/JSON import, validation, dedupe | `EnterpriseDataImportService`, `ImportJob` (in-memory staging of validated records) | core-application `integration` |
| Operational event ingestion/monitoring | `OperationalEventIngestionService`, `OperationalMonitoringService` | core-application `service` |
| Recovery plan lifecycle + human approval | `RecoveryPlan` (DRAFT→…→APPROVED), `PlanApproval`, stale twin-version check | core-domain `plan` |
| RBAC / workspace isolation | `WorkspaceAccessGuard`, `SecurityContext` (thread-local), `Permission`, `UserRole` | core-application `security` |
| Audit | `AuditGovernanceService` (signed entries) | core-application `audit` |
| Provenance | `DataProvenanceService`, decision provenance graph | `provenance` |
| Agents, ConcurGuard, simulation, optimization | Phase 3–6 modules | adapter-ai, concurguard, simulation-engine |
| Experiments | Phase 8 benchmark/what-if framework | `benchmark`, `whatif` |

Not present: any connector abstraction, outbound/inbound HTTP integration, durable event delivery, sync/reconciliation, execution workflows, execution adapters, metrics/Actuator wiring.

## 3. Gaps and design decisions
- **Connector runtime**: new `connector` domain package + registry service + persistence. Vendor logic stays out of core-domain; adapters implement an application port.
- **Durable delivery**: PostgreSQL transactional outbox (`outbox_events`, unique `(workspace_id, idempotency_key)`, `FOR UPDATE SKIP LOCKED` claim, exponential backoff, dead-letter, authorized audited replay) and inbox dedupe table. No Kafka: a single PostgreSQL instance satisfies ordering-by-creation, durability and replay needs of a modular monolith.
- **Security**: new permissions `CONNECTOR_MANAGE`, `CONNECTOR_EXECUTE`, `EVENT_REPLAY`; connectors store only `env:`/`vault:` credential references; host allow-list on every configured URI.
- **Known pre-existing limitation relevant to Phase 9/10**: `SecurityContext` is thread-local and the Vaadin views hard-code `"default-workspace"`; background workers must run under an explicit system principal. `EnterpriseDataImportService` stages parsed records in memory and loses them on restart.

## 4. Delivery status
Phases 9 and 10 are implemented as described in `PHASE_9_ARCHITECTURE_AND_IMPLEMENTATION.md` and `PHASE_10_ARCHITECTURE_AND_IMPLEMENTATION.md`; results and limitations are in the two completion reports. The gap analysis above was the plan; items that remain gaps after implementation are listed under "Known limitations" in each completion report.

## 5. Additional baseline findings recorded during implementation
* `@Theme("supplygraph")` had no theme folder (`src/main/frontend/themes/supplygraph`), so the Vaadin UI returned HTTP 500 on a clean checkout in dev mode. A minimal theme folder was added.
* No view or service wiring set `SecurityContext`; guarded services could not be called from the UI. `UiPrincipalFilter` now supplies a configured operator (not authentication).
* `audit_logs.previous_state_version / new_state_version` are `VARCHAR(32)`; new audit calls use short labels and execution auditing truncates to 32 characters.
* The Decision Centre's approver field defaulted to the free-text "VP_SUPPLY_CHAIN_OPERATIONS", which cannot be traced to a user; it now defaults to the current principal's user id.
