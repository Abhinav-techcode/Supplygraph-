package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.audit.AuditLogId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for immutable, append-only persistence of governance audit logs.
 */
public interface AuditLogRepositoryPort {

    AuditLogEntry save(AuditLogEntry entry);

    Optional<AuditLogEntry> findById(AuditLogId id);

    List<AuditLogEntry> findByWorkspaceId(WorkspaceId workspaceId);

    List<AuditLogEntry> findByTargetEntityId(String targetEntityId);

    List<AuditLogEntry> findAll();
}

