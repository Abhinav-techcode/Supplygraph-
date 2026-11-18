package com.supplygraph.application.connector;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.*;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.domain.model.connector.ConnectorId;
import com.supplygraph.domain.model.event.*;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.sync.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Applies external records to SupplyGraph's integration mirror under field-authority policy, records record-level reconciliation,
 * advances checkpoints, and forwards accepted observations into the existing operational event pipeline.
 */
@Service
public class SyncReconciliationService {
    public record SyncSummary(boolean initial, int evaluated, int autoAccepted, int conflicts, int rejectedByPolicy, int unchanged) {}

    private final FieldMirrorRepositoryPort mirrors;
    private final ReconciliationRepositoryPort reconciliations;
    private final SyncCheckpointRepositoryPort checkpoints;
    private final FieldAuthorityPolicyRepositoryPort policies;
    private final OperationalEventIngestionService events;
    private final WorkspaceAccessGuard guard;
    private final AuditGovernanceService audit;

    public SyncReconciliationService(FieldMirrorRepositoryPort mirrors, ReconciliationRepositoryPort reconciliations,
                                     SyncCheckpointRepositoryPort checkpoints, FieldAuthorityPolicyRepositoryPort policies,
                                     OperationalEventIngestionService events, WorkspaceAccessGuard guard, AuditGovernanceService audit) {
        this.mirrors = mirrors;
        this.reconciliations = reconciliations;
        this.checkpoints = checkpoints;
        this.policies = policies;
        this.events = events;
        this.guard = guard;
        this.audit = audit;
    }

    /** Callers (execution service, webhook handler) have already authorized the operation. */
    @Transactional
    public SyncSummary apply(Connector c, List<ExternalRecord> records, String correlationId) {
        List<FieldAuthorityPolicy> overrides = policies.findByWorkspace(c.workspaceId());
        boolean initial = checkpoints.find(c.id()).map(SyncCheckpoint::isInitial).orElse(true);
        int evaluated = 0, auto = 0, conflicts = 0, byPolicy = 0, unchanged = 0;
        for (ExternalRecord r : records) {
            for (Map.Entry<String, String> f : r.fields().entrySet()) {
                evaluated++;
                String field = f.getKey(), external = f.getValue();
                Optional<FieldMirror> mirror = mirrors.find(c.workspaceId(), c.id(), r.entityType(), r.id(), field);
                String internal = mirror.map(FieldMirror::internalValue).orElse(null);
                String lastSynced = mirror.map(FieldMirror::lastSyncedValue).orElse(null);
                FieldAuthority authority = authorityFor(overrides, c, r.entityType(), field);
                switch (ReconciliationEngine.decide(authority, external, internal, lastSynced)) {
                    case NO_CHANGE -> {
                        unchanged++;
                        if (mirror.isPresent() && Objects.equals(external, internal) && !Objects.equals(lastSynced, external)) {
                            mirrors.save(mirror.get().withAgreedValue(external, Instant.now()));
                        }
                    }
                    case AUTO_ACCEPT -> {
                        auto++;
                        adopt(c, r, field, external, mirror);
                        reconciliations.save(ReconciliationItem.create(c.workspaceId(), c.id(), r.entityType(), r.id(), field, external, internal,
                                lastSynced, ReconciliationStatus.AUTO_ACCEPTED, correlationId, c.isSimulated()));
                        emit(c, r.entityType(), r.id(), field, external, correlationId);
                    }
                    case REJECT_BY_POLICY -> {
                        byPolicy++;
                        reconciliations.save(ReconciliationItem.create(c.workspaceId(), c.id(), r.entityType(), r.id(), field, external, internal,
                                lastSynced, ReconciliationStatus.REJECTED_BY_POLICY, correlationId, c.isSimulated()));
                    }
                    case CONFLICT -> {
                        conflicts++;
                        if (!reconciliations.existsOpenFor(c.workspaceId(), c.id(), r.entityType(), r.id(), field, external)) {
                            reconciliations.save(ReconciliationItem.create(c.workspaceId(), c.id(), r.entityType(), r.id(), field, external, internal,
                                    lastSynced, ReconciliationStatus.CONFLICT, correlationId, c.isSimulated()));
                        }
                    }
                }
            }
        }
        SyncCheckpoint cp = checkpoints.find(c.id()).orElse(new SyncCheckpoint(c.id(), null, null, 0));
        Instant now = Instant.now();
        checkpoints.save(cp.advance(now.toString(), now));
        return new SyncSummary(initial, evaluated, auto, conflicts, byPolicy, unchanged);
    }

    @Transactional
    public ReconciliationItem resolve(String itemId, boolean acceptExternal, String rationale) {
        ReconciliationItem item = reconciliations.findById(itemId).orElseThrow(() -> new NoSuchElementException("Reconciliation item not found: " + itemId));
        guard.enforceWorkspaceAndPermission(item.workspaceId(), Permission.DATA_RECONCILE);
        var user = SecurityContext.getCurrentUser().id();
        ReconciliationItem resolved = reconciliations.save(item.resolve(acceptExternal, user.value(), rationale, Instant.now()));
        Optional<FieldMirror> mirror = mirrors.find(item.workspaceId(), item.connectorId(), item.entityType(), item.entityKey(), item.field());
        Instant now = Instant.now();
        if (acceptExternal) {
            adoptResolved(item, mirror, now);
            emit(item.workspaceId(), item.simulated(), item.entityType(), item.entityKey(), item.field(), item.externalValue(), item.correlationId());
        } else {
            mirror.ifPresent(m -> mirrors.save(m.withAcknowledgedExternal(item.externalValue(), now)));
        }
        audit.logAction(user, item.workspaceId(), AuditAction.RECONCILIATION_RESOLVED, itemId, ReconciliationStatus.CONFLICT.name(),
                resolved.status().name(), rationale, null);
        return resolved;
    }

    public List<ReconciliationItem> openConflicts(WorkspaceId ws) { guard.enforceWorkspaceAccess(ws); return reconciliations.findOpen(ws); }

    public List<ReconciliationItem> history(WorkspaceId ws, int limit) { guard.enforceWorkspaceAccess(ws); return reconciliations.findByWorkspace(ws, limit); }

    public Optional<SyncCheckpoint> checkpoint(ConnectorId id) { return checkpoints.find(id); }

    public List<FieldMirror> mirror(WorkspaceId ws, int limit) { guard.enforceWorkspaceAccess(ws); return mirrors.listByWorkspace(ws, limit); }

    /** Configures a workspace-level authority override. */
    public FieldAuthorityPolicy setPolicy(WorkspaceId ws, String entityType, String field, FieldAuthority authority) {
        guard.enforceWorkspaceAndPermission(ws, Permission.CONNECTOR_MANAGE);
        FieldAuthorityPolicy saved = policies.save(new FieldAuthorityPolicy(ws, entityType, field, authority));
        audit.logAction(SecurityContext.getCurrentUser().id(), ws, AuditAction.FIELD_POLICY_CHANGED, entityType + "." + field, "NONE", authority.name(), "Field authority override", null);
        return saved;
    }

    public List<FieldAuthorityPolicy> policies(WorkspaceId ws) { guard.enforceWorkspaceAccess(ws); return policies.findByWorkspace(ws); }

    static FieldAuthority authorityFor(List<FieldAuthorityPolicy> overrides, Connector c, String entityType, String field) {
        FieldAuthority wildcard = null;
        for (FieldAuthorityPolicy p : overrides) {
            if (!p.entityType().equals(entityType)) continue;
            if (p.field().equals(field)) return p.authority();
            if (p.field().equals("*")) wildcard = p.authority();
        }
        return wildcard != null ? wildcard : FieldAuthorityPolicy.defaultAuthority(c.category(), entityType);
    }

    private void adopt(Connector c, ExternalRecord r, String field, String value, Optional<FieldMirror> existing) {
        Instant now = Instant.now();
        FieldMirror m = existing.orElseGet(() -> new FieldMirror(c.workspaceId(), c.id(), r.entityType(), r.id(), field, null, null, now));
        mirrors.save(m.withAgreedValue(value, now));
    }

    private void adoptResolved(ReconciliationItem item, Optional<FieldMirror> existing, Instant now) {
        FieldMirror m = existing.orElseGet(() -> new FieldMirror(item.workspaceId(), item.connectorId(), item.entityType(), item.entityKey(), item.field(), null, null, now));
        mirrors.save(m.withAgreedValue(item.externalValue(), now));
    }

    private void emit(Connector c, String type, String key, String field, String value, String correlationId) {
        emit(c.workspaceId(), c.isSimulated(), type, key, field, value, correlationId);
    }

    /** Only fields with a defined monitoring semantics are forwarded; others remain mirror-only. */
    private void emit(WorkspaceId ws, boolean simulated, String type, String key, String field, String value, String correlationId) {
        EventType eventType = eventTypeFor(type, field);
        if (eventType == null) return;
        Map<String, String> payload = new HashMap<>();
        payload.put(field, value);
        payload.put("correlationId", correlationId == null ? "" : correlationId);
        payload.put("simulated", Boolean.toString(simulated));
        String idem = "sync:" + ws.value() + ":" + type + ":" + key + ":" + field + ":" + value;
        events.ingestEvent(OperationalEvent.createIngested(eventType, simulated ? EventSource.SYNTHETIC_SCENARIO : EventSource.EXTERNAL_INTEGRATION,
                Instant.now(), key, type, ws, payload,
                new EventProvenance(simulated ? "SIMULATED_CONNECTOR" : "EXTERNAL_CONNECTOR", "connector-sync", null, null, Instant.now()), idem));
    }

    static EventType eventTypeFor(String entityType, String field) {
        return switch (entityType + "." + field) {
            case "STOCK.onHandQuantity" -> EventType.INVENTORY_LEVEL_CHANGED;
            case "SHIPMENT.milestone" -> EventType.SHIPMENT_STATUS_CHANGED;
            case "SHIPMENT.delayHours" -> EventType.SUPPLIER_DELIVERY_DELAYED;
            case "SUPPLIER.capacityDropPercentage" -> EventType.SUPPLIER_CAPACITY_CHANGED;
            case "PRODUCTION_LINE.capacityChange" -> EventType.PRODUCTION_CAPACITY_CHANGED;
            case "QUALITY_HOLD.status" -> EventType.QUALITY_HOLD_RECORDED;
            default -> null;
        };
    }
}
