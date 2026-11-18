package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.*;
import com.supplygraph.adapter.persistence.repository.*;
import com.supplygraph.application.port.out.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorId;
import com.supplygraph.domain.model.sync.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresSyncPersistenceAdapter implements FieldMirrorRepositoryPort, ReconciliationRepositoryPort, SyncCheckpointRepositoryPort, FieldAuthorityPolicyRepositoryPort {
    private final SpringDataFieldMirrorRepository mirrors;
    private final SpringDataReconciliationItemRepository items;
    private final SpringDataSyncCheckpointRepository checkpoints;
    private final SpringDataFieldAuthorityPolicyRepository policies;

    public PostgresSyncPersistenceAdapter(SpringDataFieldMirrorRepository mirrors, SpringDataReconciliationItemRepository items,
                                          SpringDataSyncCheckpointRepository checkpoints, SpringDataFieldAuthorityPolicyRepository policies) {
        this.mirrors = mirrors; this.items = items; this.checkpoints = checkpoints; this.policies = policies;
    }

    static String hash(String... parts) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("\u0000", parts).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    // --- mirrors
    @Override @Transactional(readOnly = true)
    public Optional<FieldMirror> find(WorkspaceId ws, ConnectorId c, String type, String key, String field) {
        return mirrors.findById(hash(ws.value(), c.value(), type, key, field)).map(e -> new FieldMirror(WorkspaceId.of(e.getWorkspaceId()), ConnectorId.of(e.getConnectorId()),
                e.getEntityType(), e.getEntityKey(), e.getFieldName(), e.getInternalValue(), e.getLastSyncedValue(), e.getUpdatedAt()));
    }

    @Override @Transactional
    public FieldMirror save(FieldMirror m) {
        String id = hash(m.workspaceId().value(), m.connectorId().value(), m.entityType(), m.entityKey(), m.field());
        FieldMirrorJpaEntity e = mirrors.findById(id).orElseGet(FieldMirrorJpaEntity::new);
        e.setId(id); e.setWorkspaceId(m.workspaceId().value()); e.setConnectorId(m.connectorId().value());
        e.setEntityType(m.entityType()); e.setEntityKey(m.entityKey()); e.setFieldName(m.field());
        e.setInternalValue(m.internalValue()); e.setLastSyncedValue(m.lastSyncedValue()); e.setUpdatedAt(m.updatedAt());
        mirrors.save(e);
        return m;
    }

    @Override @Transactional(readOnly = true)
    public List<FieldMirror> listByWorkspace(WorkspaceId ws, int limit) {
        return mirrors.findByWorkspaceIdOrderByUpdatedAtDesc(ws.value(), PageRequest.of(0, bound(limit))).stream().map(e ->
                new FieldMirror(ws, ConnectorId.of(e.getConnectorId()), e.getEntityType(), e.getEntityKey(), e.getFieldName(),
                        e.getInternalValue(), e.getLastSyncedValue(), e.getUpdatedAt())).toList();
    }

    // --- reconciliation items
    @Override @Transactional
    public ReconciliationItem save(ReconciliationItem i) {
        ReconciliationItemJpaEntity e = items.findById(i.id()).orElseGet(ReconciliationItemJpaEntity::new);
        e.setId(i.id()); e.setWorkspaceId(i.workspaceId().value()); e.setConnectorId(i.connectorId().value());
        e.setEntityType(i.entityType()); e.setEntityKey(i.entityKey()); e.setFieldName(i.field());
        e.setExternalValue(i.externalValue()); e.setInternalValue(i.internalValue()); e.setLastSyncedValue(i.lastSyncedValue());
        e.setStatus(i.status().name()); e.setResolvedBy(i.resolvedBy()); e.setResolutionRationale(i.resolutionRationale());
        e.setCreatedAt(i.createdAt()); e.setResolvedAt(i.resolvedAt()); e.setCorrelationId(i.correlationId()); e.setSimulated(i.simulated());
        items.save(e);
        return i;
    }

    @Override @Transactional(readOnly = true)
    public Optional<ReconciliationItem> findById(String id) { return items.findById(id).map(this::toDomain); }

    @Override @Transactional(readOnly = true)
    public List<ReconciliationItem> findOpen(WorkspaceId ws) {
        return items.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(ws.value(), ReconciliationStatus.CONFLICT.name()).stream().map(this::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ReconciliationItem> findByWorkspace(WorkspaceId ws, int limit) {
        return items.findByWorkspaceIdOrderByCreatedAtDesc(ws.value(), PageRequest.of(0, bound(limit))).stream().map(this::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public boolean existsOpenFor(WorkspaceId ws, ConnectorId c, String type, String key, String field, String externalValue) {
        return items.existsByWorkspaceIdAndConnectorIdAndEntityTypeAndEntityKeyAndFieldNameAndExternalValueAndStatus(
                ws.value(), c.value(), type, key, field, externalValue, ReconciliationStatus.CONFLICT.name());
    }

    // --- checkpoints
    @Override @Transactional(readOnly = true)
    public Optional<SyncCheckpoint> find(ConnectorId id) {
        return checkpoints.findById(id.value()).map(e -> new SyncCheckpoint(id, e.getCursorValue(), e.getLastSyncedAt(), e.getSyncCount()));
    }

    @Override @Transactional
    public SyncCheckpoint save(SyncCheckpoint c) {
        SyncCheckpointJpaEntity e = checkpoints.findById(c.connectorId().value()).orElseGet(SyncCheckpointJpaEntity::new);
        e.setConnectorId(c.connectorId().value()); e.setCursorValue(c.cursor()); e.setLastSyncedAt(c.lastSyncedAt()); e.setSyncCount(c.syncCount());
        checkpoints.save(e);
        return c;
    }

    // --- policies
    @Override @Transactional(readOnly = true)
    public List<FieldAuthorityPolicy> findByWorkspace(WorkspaceId ws) {
        return policies.findByWorkspaceId(ws.value()).stream().map(e ->
                new FieldAuthorityPolicy(ws, e.getEntityType(), e.getFieldName(), FieldAuthority.valueOf(e.getAuthority()))).toList();
    }

    @Override @Transactional
    public FieldAuthorityPolicy save(FieldAuthorityPolicy p) {
        String id = hash(p.workspaceId().value(), p.entityType(), p.field());
        FieldAuthorityPolicyJpaEntity e = policies.findById(id).orElseGet(FieldAuthorityPolicyJpaEntity::new);
        e.setId(id); e.setWorkspaceId(p.workspaceId().value()); e.setEntityType(p.entityType()); e.setFieldName(p.field()); e.setAuthority(p.authority().name());
        policies.save(e);
        return p;
    }

    private ReconciliationItem toDomain(ReconciliationItemJpaEntity e) {
        return new ReconciliationItem(e.getId(), WorkspaceId.of(e.getWorkspaceId()), ConnectorId.of(e.getConnectorId()), e.getEntityType(), e.getEntityKey(),
                e.getFieldName(), e.getExternalValue(), e.getInternalValue(), e.getLastSyncedValue(), ReconciliationStatus.valueOf(e.getStatus()),
                e.getResolvedBy(), e.getResolutionRationale(), e.getCreatedAt(), e.getResolvedAt(), e.getCorrelationId(), e.isSimulated());
    }

    private static int bound(int limit) { return Math.min(Math.max(limit, 1), 1000); }
}
