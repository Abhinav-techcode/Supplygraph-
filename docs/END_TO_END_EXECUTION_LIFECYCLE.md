# End-to-End Lifecycle (as implemented and as tested)

| # | Step | Implemented by | Verified by |
|---|---|---|---|
| 1–2 | Register a connector, configure a sandbox adapter | `ConnectorRegistryService` | `Phase9EndToEndIntegrationTest`, `Phase10EndToEndIntegrationTest` (**SIMULATED** sandbox) |
| 3–5 | Receive an operational event, validate, update the twin where authorized | Webhook (HMAC) or sandbox sync → outbox → `SyncReconciliationService` → `OperationalEventIngestionService` | Phase 9 E2E (events marked `SYNTHETIC_SCENARIO` when simulated) |
| 6–8 | Detect disruption, assess impact, recommend | Existing Phase 3–8 services | **Not re-driven by the new E2E tests**; the tests insert a disruption and an already-approved plan directly |
| 9–11 | ConcurGuard, simulate, human approval | Existing `RecoveryPlanningApplicationService` | Re-validated read-only at conversion; approval itself is constructed in tests via the domain |
| 12–13 | Plan → workflow, validate dependencies | `RecoveryPlanConversionService`, `TaskGraph` | Phase 10 E2E + unit tests |
| 14 | Execute eligible operations | `ExecutionOrchestrationService` + `SandboxExecutionAdapter` | **SIMULATED** only |
| 15 | Acknowledgements | `ExecutionAcknowledgementService` | duplicate/late acks tested; entered via service call |
| 16 | Reconcile outcomes | `ExecutionReconciliationService` | unknown-outcome and observation tests |
| 17 | Update twin from validated observations | `TwinFeedback` | verified with a non-simulated scripted adapter in unit tests; **not exercised against a real system** |
| 18–19 | Detect deviation, bounded reassessment | `ClosedLoopService`, `ReassessmentGovernor` | unit + scenario S6 |
| 20 | Provenance | `execution_events` (correlation ids), signed audit entries, evidence | Phase 10 E2E |

Anything labelled SIMULATED did not involve an external system.
