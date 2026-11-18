# Research Methodology — Phase 9 & 10 Evaluation

## What is measured
`ExecutionResearchScenariosTest` (core-application) runs deterministic, seeded scenarios and writes `supplygraph-core-application/target/research/execution-scenarios.json` with: software version, git commit (and dirty-file count), seed, digital-twin snapshot description, scenario id, execution mode, connector mode, trial count, invariant violations and summary statistics.

| Id | Question | Trials | Violations (last run) |
|---|---|---|---|
| S1 dependency enforcement | Does a task ever run before its predecessors completed, or after a failed predecessor? (random DAGs of 3–8 tasks, 20 % seeded permanent failures) | 200 | 0 |
| S2 duplicate suppression | Does a message delivered 1–5 times in shuffled order have exactly one effect? (6,386 deliveries, 4,268 suppressed) | 200 | 0 |
| S3 approval enforcement | Can any of the 9 roles approve/start outside policy (separation of duties, permissions)? | 18 checks | 0 |
| S4 unknown outcomes | Is an unknown outcome ever re-sent without reconciliation? | 100 | 0 |
| S5 idempotent bounded retries | One idempotency key and ≤ 3 sends under always-failing retries? | 100 | 0 |
| S6 bounded replanning | Proposals only for real deviation ≥ 15 % and depth < 2, never duplicated? (51 proposals raised) | 150 | 0 |

Other evidence, in tests rather than the scenario runner: durable outbox, atomic enqueue/rollback, concurrent claims, inbox concurrency, worker-crash recovery and workspace isolation against real PostgreSQL (`Phase9PersistenceIntegrationTest`, `Phase9EndToEndIntegrationTest`, `Phase10PersistenceIntegrationTest`, `Phase10EndToEndIntegrationTest`).

## Seeds and reproducibility
Base seed `20261118`; scenarios use `seed`, `seed+1`, `seed+4`, `seed+5`. Adapter behaviour is scripted, not random. Re-running yields identical counts; only wall-clock fields vary.

## Limitations (read before citing any number)
* Execution mode is **SIMULATED**; ports are in-memory for the scenario runner. These results show that coded invariants hold across generated cases. They say nothing about real ERP/WMS/TMS behaviour.
* Workflow latency (≈0.8 ms mean per generated workflow) measures in-process software overhead only, not decision-to-execution latency in an organisation (which is dominated by human approval).
* Connector delivery reliability, synchronization recovery and reconciliation *accuracy* against real systems were **not** evaluated; no dataset or real endpoint was available. Reconciliation correctness is covered by deterministic unit/integration tests (`ReconciliationEngineTest`, `SyncReconciliationServiceTest`).
* No comparison with other systems was performed. No claim of superiority, significance or generalisation is made; none of these figures are statistical tests.
* Statistics reported are mean, standard deviation, p50 and p95 of recorded timings only.
