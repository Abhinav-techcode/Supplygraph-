package com.supplygraph.application.connector;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.*;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.event.EventSource;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.security.*;
import com.supplygraph.domain.model.sync.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyncReconciliationServiceTest {
    private final WorkspaceId ws = WorkspaceId.of("ws-1");
    private final Map<String, FieldMirror> mirrors = new HashMap<>();
    private final Map<String, ReconciliationItem> items = new LinkedHashMap<>();
    private final List<FieldAuthorityPolicy> overrides = new ArrayList<>();
    private final Map<ConnectorId, SyncCheckpoint> cps = new HashMap<>();
    private OperationalEventIngestionService events;
    private SyncReconciliationService service;
    private Connector tms, wms;

    private static String key(WorkspaceId w, ConnectorId c, String t, String k, String f) { return w.value() + c + t + k + f; }

    @BeforeEach void setUp() {
        FieldMirrorRepositoryPort mp = new FieldMirrorRepositoryPort() {
            public Optional<FieldMirror> find(WorkspaceId w, ConnectorId c, String t, String k, String f) { return Optional.ofNullable(mirrors.get(key(w, c, t, k, f))); }
            public FieldMirror save(FieldMirror m) { mirrors.put(key(m.workspaceId(), m.connectorId(), m.entityType(), m.entityKey(), m.field()), m); return m; }
            public List<FieldMirror> listByWorkspace(WorkspaceId w, int l) { return List.copyOf(mirrors.values()); }
        };
        ReconciliationRepositoryPort rp = new ReconciliationRepositoryPort() {
            public ReconciliationItem save(ReconciliationItem i) { items.put(i.id(), i); return i; }
            public Optional<ReconciliationItem> findById(String id) { return Optional.ofNullable(items.get(id)); }
            public List<ReconciliationItem> findOpen(WorkspaceId w) { return items.values().stream().filter(i -> i.status().isOpen()).toList(); }
            public List<ReconciliationItem> findByWorkspace(WorkspaceId w, int l) { return List.copyOf(items.values()); }
            public boolean existsOpenFor(WorkspaceId w, ConnectorId c, String t, String k, String f, String ev) {
                return items.values().stream().anyMatch(i -> i.status().isOpen() && i.connectorId().equals(c) && i.entityType().equals(t) && i.entityKey().equals(k) && i.field().equals(f) && Objects.equals(i.externalValue(), ev));
            }
        };
        SyncCheckpointRepositoryPort cp = new SyncCheckpointRepositoryPort() {
            public Optional<SyncCheckpoint> find(ConnectorId id) { return Optional.ofNullable(cps.get(id)); }
            public SyncCheckpoint save(SyncCheckpoint c) { cps.put(c.connectorId(), c); return c; }
        };
        FieldAuthorityPolicyRepositoryPort pp = new FieldAuthorityPolicyRepositoryPort() {
            public List<FieldAuthorityPolicy> findByWorkspace(WorkspaceId w) { return overrides; }
            public FieldAuthorityPolicy save(FieldAuthorityPolicy p) { overrides.add(p); return p; }
        };
        events = mock(OperationalEventIngestionService.class);
        service = new SyncReconciliationService(mp, rp, cp, pp, events, new WorkspaceAccessGuard(), mock(AuditGovernanceService.class));
        ConnectorConfiguration cfg = new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 1);
        tms = Connector.register(ws, "TMS", ConnectorCategory.TMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox").configure(cfg).activate();
        wms = Connector.register(ws, "WMS", ConnectorCategory.WMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox").configure(cfg).activate();
        login(UserRole.SUPPLY_CHAIN_MANAGER, ws);
    }
    @AfterEach void tearDown() { SecurityContext.clear(); }
    private void login(UserRole r, WorkspaceId w) { SecurityContext.setCurrentUser(UserProfile.create("U1", "u", "u@x.io", "U", r, Set.of(w))); }
    private static ExternalRecord shipment(String id, String milestone) { return new ExternalRecord(id, "SHIPMENT", Map.of("milestone", milestone)); }

    @Test void initialSyncAdoptsAuthoritativeValuesAndForwardsEventsMarkedSimulated() {
        var s = service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "corr");
        assertThat(s.initial()).isTrue();
        assertThat(s.autoAccepted()).isEqualTo(1);
        ArgumentCaptor<OperationalEvent> ev = ArgumentCaptor.forClass(OperationalEvent.class);
        verify(events).ingestEvent(ev.capture());
        assertThat(ev.getValue().getEventType()).isEqualTo(EventType.SHIPMENT_STATUS_CHANGED);
        assertThat(ev.getValue().getSource()).isEqualTo(EventSource.SYNTHETIC_SCENARIO);
        assertThat(ev.getValue().getPayload()).containsEntry("simulated", "true");
        assertThat(service.checkpoint(tms.id())).isPresent();
        assertThat(service.apply(tms, List.of(shipment("S1", "DELIVERED")), "c2").initial()).isFalse();
    }

    @Test void unchangedSecondSyncProducesNoNewDecisionsOrEvents() {
        service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "c");
        var s = service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "c");
        assertThat(s.unchanged()).isEqualTo(1);
        verify(events, times(1)).ingestEvent(any());
    }

    @Test void connectorThatDoesNotOwnTheEntityCannotOverwriteInternalData() {
        var s = service.apply(wms, List.of(shipment("S1", "DISPATCHED")), "c");   // WMS is not authoritative for SHIPMENT
        assertThat(s.rejectedByPolicy()).isEqualTo(1);
        assertThat(mirrors).isEmpty();
        verify(events, never()).ingestEvent(any());
        assertThat(items.values()).extracting(ReconciliationItem::status).containsExactly(ReconciliationStatus.REJECTED_BY_POLICY);
    }

    @Test void divergenceCreatesOneOpenConflictWithoutOverwriting() {
        service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "c");
        FieldMirror m = mirrors.values().iterator().next();
        mirrors.put(key(m.workspaceId(), m.connectorId(), m.entityType(), m.entityKey(), m.field()), m.withInternalValue("HELD_BY_OPERATOR", java.time.Instant.now()));
        var s1 = service.apply(tms, List.of(shipment("S1", "DELIVERED")), "c");
        var s2 = service.apply(tms, List.of(shipment("S1", "DELIVERED")), "c");   // repeat: no duplicate conflict
        assertThat(s1.conflicts()).isEqualTo(1);
        assertThat(s2.conflicts()).isEqualTo(1);
        assertThat(service.openConflicts(ws)).hasSize(1);
        assertThat(mirrors.values().iterator().next().internalValue()).isEqualTo("HELD_BY_OPERATOR");
    }

    @Test void humanCanAcceptExternalOrKeepInternal() {
        service.apply(tms, List.of(shipment("S1", "DISPATCHED"), shipment("S2", "DISPATCHED")), "c");
        for (FieldMirror m : List.copyOf(mirrors.values())) mirrors.put(key(m.workspaceId(), m.connectorId(), m.entityType(), m.entityKey(), m.field()), m.withInternalValue("HELD", java.time.Instant.now()));
        service.apply(tms, List.of(shipment("S1", "DELIVERED"), shipment("S2", "DELIVERED")), "c");
        List<ReconciliationItem> open = service.openConflicts(ws);
        assertThat(open).hasSize(2);
        ReconciliationItem a = open.get(0), b = open.get(1);
        assertThat(service.resolve(a.id(), true, "carrier confirmed").status()).isEqualTo(ReconciliationStatus.ACCEPTED);
        assertThat(service.resolve(b.id(), false, "we hold it").status()).isEqualTo(ReconciliationStatus.REJECTED);
        assertThat(mirrors.get(key(ws, tms.id(), "SHIPMENT", a.entityKey(), "milestone")).internalValue()).isEqualTo("DELIVERED");
        assertThat(mirrors.get(key(ws, tms.id(), "SHIPMENT", b.entityKey(), "milestone")).internalValue()).isEqualTo("HELD");
        // kept-internal does not re-conflict with the same external value
        assertThat(service.apply(tms, List.of(shipment(b.entityKey(), "DELIVERED")), "c").conflicts()).isZero();
        assertThatThrownBy(() -> service.resolve(a.id(), true, "again")).isInstanceOf(IllegalStateException.class);
    }

    @Test void resolutionRequiresPermissionWorkspaceAndRationale() {
        service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "c");
        FieldMirror m = mirrors.values().iterator().next();
        mirrors.put(key(m.workspaceId(), m.connectorId(), m.entityType(), m.entityKey(), m.field()), m.withInternalValue("HELD", java.time.Instant.now()));
        service.apply(tms, List.of(shipment("S1", "DELIVERED")), "c");
        String id = service.openConflicts(ws).get(0).id();
        login(UserRole.ANALYST, ws);
        assertThatThrownBy(() -> service.resolve(id, true, "x")).isInstanceOf(UnauthorizedOperationException.class);
        login(UserRole.SUPPLY_CHAIN_MANAGER, WorkspaceId.of("other"));
        assertThatThrownBy(() -> service.resolve(id, true, "x")).isInstanceOf(WorkspaceAccessDeniedException.class);
        login(UserRole.SUPPLY_CHAIN_MANAGER, ws);
        assertThatThrownBy(() -> service.resolve(id, true, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.openConflicts(ws)).hasSize(1);
    }

    @Test void workspaceOverrideChangesAuthorityAndWildcardApplies() {
        service.setPolicy(ws, "SHIPMENT", "*", FieldAuthority.INTERNAL);
        assertThat(service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "c").rejectedByPolicy()).isEqualTo(1);
        overrides.clear();
        service.setPolicy(ws, "SHIPMENT", "milestone", FieldAuthority.EXTERNAL);
        service.setPolicy(ws, "SHIPMENT", "*", FieldAuthority.INTERNAL);
        assertThat(service.apply(tms, List.of(shipment("S1", "DISPATCHED")), "c").autoAccepted()).isEqualTo(1);
    }

    @Test void onlyFieldsWithMonitoringSemanticsEmitEvents() {
        ConnectorConfiguration cfg = new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 1);
        Connector erp = Connector.register(ws, "ERP", ConnectorCategory.ERP, ConnectorMode.SANDBOX_SIMULATED, "sandbox").configure(cfg).activate();
        service.apply(erp, List.of(new ExternalRecord("PO-1", "PURCHASE_ORDER", Map.of("status", "OPEN"))), "c");
        verify(events, never()).ingestEvent(any());
        assertThat(service.mirror(ws, 10)).hasSize(1);
    }
}
