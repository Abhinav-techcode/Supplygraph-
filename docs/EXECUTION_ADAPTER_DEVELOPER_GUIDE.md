# Execution Adapter Developer Guide

Implement `com.supplygraph.application.execution.ExecutionAdapter` and register it as a Spring bean.

| Contract element | Requirement |
|---|---|
| `adapterId()` / `simulated()` | `simulated()` must be `true` for anything that does not act on a real system; it propagates to attempts, evidence, tasks and the workflow mode |
| `supportedTypes()` | `TaskType`s the adapter can execute; `AdapterSelector` picks per type |
| `timeout()` | enforced by the orchestration service (+1 s grace); a timeout becomes an **unknown outcome** |
| `checkPreconditions(request)` | return violations; any violation fails the task permanently *before* a request is sent |
| `execute(request)` | never throw for remote problems. Honour `request.idempotencyKey()` (stable across retries of the same task: `task:<taskId>`) |
| Responses | `ACKNOWLEDGED` = accepted, **not done** (task waits in `AWAITING_ACK`); `COMPLETED_CONFIRMED` = authoritative synchronous completion; `REJECTED`/`PERMANENT_ERROR`/`AUTHENTICATION_FAILURE` = permanent failure; `RETRYABLE_ERROR` = bounded retry; `TIMEOUT_UNKNOWN` = unknown outcome, needs reconciliation |
| `compensate(request)` | implement only where a real business compensation exists; default refuses |
| Authorization/audit | adapters do not authorize; `ExecutionOrchestrationService` checks `EXECUTION_RUN`, separation of duties and writes audit/timeline entries |

Rules enforced by ArchUnit: adapters must not depend on repository ports or application services, and must not touch persistence.

## LIVE adapters
LIVE adapters are used only when the workspace policy has `allowLiveExecution=true` (default false). **No LIVE execution adapter exists in this repository**; with the default policy only the SIMULATED sandbox adapter runs. A future adapter must be verified against a real or documented test endpoint before it is described as real.

## Sandbox behaviours
`SandboxExecutionAdapter` reads the task parameter `sim.behavior`: `ACK` (default), `CONFIRM`, `FAIL_RETRYABLE_ONCE`, `FAIL_RETRYABLE_ALWAYS`, `FAIL_PERMANENT`, `REJECT`, `AUTH_FAILURE`, `TIMEOUT`, `THROW`. It is idempotent per key and every message is prefixed `[SIMULATED]`.
