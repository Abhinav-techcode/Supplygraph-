# Phase 10 Completion Report — Governed Closed-Loop Execution

Verification date 2026-11-18 · base commit `12ed537` plus uncommitted changes · branch `main` · **nothing committed or pushed**.

## Build and test results (measured)
`JAVA_HOME=~/.jdks/jdk21/Contents/Home ./mvnw -o clean verify` → **BUILD SUCCESS**; 381 tests total, 0 failures (see Phase 9 report for the per-module breakdown).
Phase 10 tests: `ExecutionDomainTest` (20: state machines, graph, cycles, conditional tasks, closed loop, governor), `ExecutionConversionAndApprovalTest` (14), `ExecutionEngineTest` (21: dependency enforcement, ack ≠ completion, retries, unknown outcomes, timeouts/crashes, duplicate/late acks, restart recovery, compensation, cancellation, stale start, timeline), `ClosedLoopAndAdvisoryTest` (8 incl. prompt-injection text treated as inert data), `ExecutionResearchScenariosTest` (6 scenarios), `SandboxExecutionAdapterTest` (5), `Phase10PersistenceIntegrationTest` (7, PostgreSQL), `Phase10EndToEndIntegrationTest` (10, PostgreSQL + full Spring context, including a concurrent double-start), `Phase9And10ArchitectureTests` (9 ArchUnit rules).

## Acceptance criteria
| Criterion | Status |
|---|---|
| Workflows from approved plans | yes; blocked (not executed) on stale twin, ConcurGuard conflicts, untraceable approver, old approval, cycles |
| Dependencies enforced | yes; graph + 200 randomized DAG trials, 0 violations |
| Human approval cannot be bypassed | READY requires approvals; SoD and permissions server-side; tested for all roles |
| Attempts and acknowledgements persisted | yes (`execution_attempts`, `execution_evidence`) |
| Failures handled safely | retryable/permanent/unknown/reconciliation distinguished; unknown outcomes never auto-retried |
| Digital twin updated from validated observations | only for non-simulated evidence, via the monitoring pipeline; verified with a scripted non-simulated adapter in unit tests, **never against a real system** |
| Deviations detected; bounded governed reassessment | yes; human decision required; hands off to the Decision Centre |
| Vaadin views end to end | 12 views; routes verified in a real browser; one UI workflow (connector registration) was clicked through; the other views were constructed in tests and loaded, **not individually exercised by hand** |
| PostgreSQL tests, architecture tests | pass |
| Docs match implementation | see limitations |

## Migration
`V11__phase10_execution.sql` — additive tables `execution_workflows`, `execution_tasks`, `execution_approvals`, `execution_attempts`, `execution_evidence`, `execution_events`, `execution_policies`, `plan_actuals`, `reassessment_requests`.

## Known limitations
* **Every execution in tests and in the UI is SIMULATED.** No LIVE execution adapter (including ERP command integration) exists; `allowLiveExecution` therefore has no real adapter to select.
* Acknowledgements are entered via UI/service; there is no inbound HTTP endpoint for them. The planner does not generate conditional, quality-inspection or material-substitution tasks (supported by the model, not produced). Independent tasks run sequentially, not on parallel threads.
* The agent layer is only a read-only, deterministic advisory boundary; no LLM or existing agent class is invoked from execution. Approving a reassessment records a decision and does not regenerate a plan.
* Concurrency control is an in-process per-workflow lock plus stale-write rejection: correct for one application instance; multi-instance deployments need a database lock.
* End-to-end tests insert the disruption and an approved plan directly; the upstream recommendation/simulation/approval steps were not re-driven through their UIs in these tests.
* Research scenarios prove invariants on generated cases, not real-world performance (see `RESEARCH_METHODOLOGY_PHASE_9_10.md`).
