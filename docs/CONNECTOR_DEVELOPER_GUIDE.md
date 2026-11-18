# Connector Developer Guide

## Add a connector adapter
1. Implement `com.supplygraph.application.connector.ConnectorAdapter` in `supplygraph-adapter-integration` (or a new adapter module). Return a failure `ConnectorCallResult` for remote problems; never throw for them.
2. Choose a unique `adapterType()`; `Connector.adapterType` selects the adapter. Duplicate types fail application start.
3. Resolve secrets only through `CredentialResolver` at call time; never log or persist them.
4. Every outbound call must go through `OutboundUrlGuard.check(uri, config.allowedHosts())`, disable redirects, and bound time and bytes using the connector's `timeoutMillis` and `maxResponseBytes` (see `RestConnectorAdapter`).
5. Map failures to `ErrorCategory`: transient network/5xx/429/timeout → `retryable=true`; authentication, policy, malformed payload, oversized response → `retryable=false`.
6. Return `ExternalRecord`s (`{"id","entityType","fields":{…}}`) for synchronization; `SyncReconciliationService` applies field-authority policy. Do not write to the twin yourself.
7. Do not import vendor types into `domain` or `application`; ArchUnit (`Phase9And10ArchitectureTests`) enforces this.

## Test it
* Unit-test against a local `com.sun.net.httpserver.HttpServer` bound to loopback and construct `OutboundUrlGuard(true)` — **loopback is allowed only in tests**. See `RestConnectorAdapterTest`.
* Label anything that does not contact a real system as simulated; a connector registered `SANDBOX_SIMULATED` must never produce data presented as real.

## Webhook senders
Sign `"{unixSeconds}.{rawBody}"` with HMAC-SHA256 using the shared secret referenced by the connector, send hex in `X-SG-Signature`, the same seconds in `X-SG-Timestamp`, and a stable `X-SG-Delivery` id per logical message. Retrying the same delivery id is safe.

## Payload shape
```json
[{"id":"SHP-1","entityType":"SHIPMENT","fields":{"milestone":"DISPATCHED","delayHours":"6"}}]
```
Records without a non-blank `id`, `entityType`, or at least one scalar field are counted as rejected.
