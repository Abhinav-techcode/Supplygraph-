# Phase 9 — Enterprise Integration Runtime: Architecture and Implementation

Everything below describes code that exists in this repository. Where something is *not* implemented it says so.

## 1. Modules and packages
| Layer | Location | Contents |
|---|---|---|
| Domain | `supplygraph-core-domain` → `domain.model.connector`, `.outbox`, `.sync` | `Connector`, `ConnectorConfiguration`, `CredentialReference`, `ConnectorExecution`, `OutboxEvent`, `BackoffPolicy`, `FieldMirror`, `ReconciliationItem`, `ReconciliationEngine`, `FieldAuthorityPolicy`, `SyncCheckpoint` |
| Application | `supplygraph-core-application` → `application.connector`, `.outbox` | `ConnectorRegistryService`, `ConnectorExecutionService`, `WebhookIngestionService`, `WebhookEventHandler`, `SyncReconciliationService`, `RuntimeOperationsService`, `OutboxService`, `InboxService`, `OutboundUrlGuard`, `WebhookSignatureVerifier`; ports `ConnectorAdapter`, `CredentialResolver`, `RecordPayloadInspector`, `*RepositoryPort` |
| Adapters | **new module** `supplygraph-adapter-integration` | `RestConnectorAdapter` (java.net.http), `SandboxConnectorAdapter` (SIMULATED), `JsonRecordPayloadInspector`, `EnvCredentialResolver`, `OutboxDispatcherWorker`, `IntegrationRuntimeMetrics`, `OutboxWorkerHealthIndicator` |
| Persistence | `supplygraph-adapter-persistence` | Flyway `V8`–`V10`; JPA adapters for connectors, outbox/inbox, executions, sync state |
| Web | `supplygraph-web` | `WebhookController`, `UiPrincipalFilter`, 12 Vaadin views under `views.integration` |

## 2. Connector lifecycle
`REGISTERED → CONFIGURED → ACTIVE ⇄ INACTIVE` (invalid transitions throw). Every transition is audited through `AuditGovernanceService`.
* `ConnectorMode` is `SANDBOX_SIMULATED` or `LIVE`. Simulated connectors are labelled everywhere (registry, executions, reconciliation items, events use `EventSource.SYNTHETIC_SCENARIO` and a `simulated=true` payload flag).
* Credentials: only a `CredentialReference` (`env:NAME` or `vault:path`) is stored. Raw secrets are rejected at construction. Secrets are resolved at call time by `CredentialResolver` (`EnvCredentialResolver` supports `env:` only; `vault:` is unsupported and resolves to nothing).
* Configuration validation: http/https scheme, host must be in `allowedHosts`, no embedded `user:pass@`, timeout 100–60000 ms, response cap ≤ 10 MiB, retries 0–10.

## 3. Executions and retries
`ConnectorExecutionService` records every run (`connector_executions`): connector, workspace, operation, trigger, status, timestamps, received/processed/rejected counts, error category, attempt, retry lineage, correlation id, `simulated`, requester. "Processed" means *passed payload validation and was handed to reconciliation*, not "changed the twin".
* `retry(executionId)` is allowed only for `FAILED_RETRYABLE` runs within the connector's `maxRetries`, and only once per execution. `AUTHENTICATION`, `POLICY_BLOCKED`, `INVALID_RESPONSE`, unexpected adapter exceptions are permanent.
* Authorization: `CONNECTOR_EXECUTE` in the connector's own workspace (loaded first, then checked: IDOR safe).

## 4. Reliable event delivery (PostgreSQL)
* **Transactional outbox** — `OutboxService.enqueue` is `@Transactional(MANDATORY)`; the row commits or rolls back with the business change (tested against PostgreSQL).
* **Idempotency** — unique `(workspace_id, idempotency_key)`; `INSERT … ON CONFLICT DO NOTHING`.
* **Worker** — `claimDue` uses `SELECT … FOR UPDATE SKIP LOCKED` so concurrent workers never share an event. Handlers run *outside* the claim transaction. Retryable failure → exponential backoff (2 s base, 10 min cap, 5 attempts); permanent failure or exhaustion → `DEAD_LETTER`. Events stuck `IN_FLIGHT` > 5 minutes are recovered to `PENDING` on the next dispatch.
* **Inbox** — `InboxService.processOnce` records `(workspace, consumer, message)` in the same transaction as the effect, so duplicate delivery has no second effect; a failing effect rolls the inbox row back so a retry is not suppressed.
* **Replay** — dead letters only, requires `EVENT_REPLAY` in the event's workspace and a rationale; audited; the original idempotency key is kept so consumers deduplicate.
* **Ordering** — events are claimed oldest first; strict per-aggregate ordering is *not* guaranteed.
* **Worker lifecycle** — `OutboxDispatcherWorker` is a single non-daemon scheduled thread (`supplygraph.outbox.worker.enabled`, default true in `application.yml`), `@PreDestroy` waits up to 15 s, it never throws, and reports to `/actuator/health` (`outboxWorker`) and Micrometer gauges. Pending work survives restart because it lives in PostgreSQL.

## 5. Webhooks
`POST /api/webhooks/{connectorId}` with `X-SG-Timestamp`, `X-SG-Signature` (hex HMAC-SHA256 over `"{timestamp}.{body}"`), optional `X-SG-Delivery`. Constant-time compare; ±5 minute window; replay detection through the inbox (delivery id, else the signature). Responses are deliberately coarse: `202` (accepted or idempotent replay), `401` (any rejection, including unknown connector), `500` without detail. The endpoint is exempt from the UI principal filter and authenticates by HMAC only. Accepted payloads are validated, durably enqueued, and applied by `WebhookEventHandler` through reconciliation.

## 6. Synchronization and reconciliation
`SyncReconciliationService.apply` evaluates every field of every record as a three-way comparison (`ReconciliationEngine`): external value, SupplyGraph internal value, value at last sync.
| Situation | Decision |
|---|---|
| external == internal | no change |
| only external changed, source authoritative | auto-accept, mirror updated, event forwarded |
| only external changed, SupplyGraph authoritative | rejected by policy, recorded |
| internal changed, external unchanged | keep internal |
| both changed | **conflict**, never auto-resolved; human resolves with rationale (`DATA_RECONCILE`) |
Default authority: a connector is authoritative only for entity types its category owns (ERP→`PURCHASE_ORDER`, WMS→`STOCK`, TMS→`SHIPMENT`, supplier systems→`SUPPLIER`, MES→`PRODUCTION_LINE`, QMS→`QUALITY_HOLD`, PLM→`PART_REVISION`); everything else (including recovery plans and simulations) is INTERNAL. Workspace overrides per `(entityType, field|*)` are configurable. A `SyncCheckpoint` records the last sync (initial vs incremental). Only fields with defined monitoring semantics (`STOCK.onHandQuantity`, `SHIPMENT.milestone`, `SHIPMENT.delayHours`, `SUPPLIER.capacityDropPercentage`, `PRODUCTION_LINE.capacityChange`, `QUALITY_HOLD.status`) become `OperationalEvent`s through the existing ingestion/monitoring pipeline; others stay in the mirror.
**Limitation:** the twin has no purchase-order or shipment aggregates, so SupplyGraph's "internal value" for external fields lives in the `field_mirrors` table.

## 7. Security
* Permissions `CONNECTOR_MANAGE`, `CONNECTOR_EXECUTE`, `EVENT_REPLAY`, `DATA_RECONCILE` (added to `Permission`; granted to `SUPPLY_CHAIN_MANAGER` and admins).
* SSRF: `OutboundUrlGuard` — https only (http only when the test-only loopback flag is set), host allow-list, every resolved address must be publicly routable (loopback, link-local incl. cloud metadata, private, CGNAT, multicast, IPv6 ULA rejected), no credentials in URL, redirects disabled, bounded response, per-request timeout. **Known gap:** validation and connection resolve DNS separately (rebinding window).
* No second authentication system. `UiPrincipalFilter` sets the thread-local `SecurityContext` from a configured operator (see §9).

## 8. Observability
`RuntimeOperationsService` derives, from persisted data only: success/failure counts, import rejection rate, average sync latency, retry counts, error categories, outbox counts, average delivery latency, event failure rate (`dead-letter / (delivered + dead-letter)`), per-connector availability. Missing data is shown as *unknown*, never zero. Micrometer gauges `supplygraph.outbox.events{status}` and worker gauges are bound by `IntegrationRuntimeMetrics`. Correlation ids are persisted on executions, outbox rows and timeline events. **Not implemented:** MDC structured logging, Prometheus registry wiring, circuit breakers (bounded retries and timeouts are used instead), scheduled connector polling (sync is manual or webhook-driven).

## 9. Vaadin views (all backed by services)
`/integration-centre`, `/connectors`, `/connector-config`, `/connector-health`, `/synchronization`, `/sync-history`, `/event-delivery`, `/dead-letters`, `/event-replay`, `/reconciliation`, `/integration-security`, `/runtime-operations`.
**There is no login system in SupplyGraph (pre-existing).** The UI principal is `supplygraph.ui.default-role` (default `SUPPLY_CHAIN_MANAGER`, workspace `default-workspace`); a role switcher appears only when `SUPPLYGRAPH_UI_DEMO_ROLE_SWITCHER=true` and is badged "not authentication". Services still enforce permissions and workspace isolation server-side against that principal. Do not expose this build on an untrusted network.

## 10. Honest connector inventory
| Connector | Status |
|---|---|
| `sandbox` | implemented, **SIMULATED**, deterministic, no I/O |
| `rest` (generic HTTP/JSON) | implemented; verified **only** against an in-process local test server (`RestConnectorAdapterTest`), never against a vendor system |
| Vendor ERP / PLM / WMS / TMS / MES / QMS adapters | **not implemented** |
| CSV/JSON file import | Phase 7 service, not routed through the connector runtime |
