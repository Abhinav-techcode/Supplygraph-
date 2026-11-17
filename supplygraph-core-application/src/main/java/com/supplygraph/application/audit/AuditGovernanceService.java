package com.supplygraph.application.audit;

import com.supplygraph.application.port.out.AuditLogRepositoryPort;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.audit.AuditLogId;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service managing immutable governance audit logs, cryptographic tamper-evidence verification,
 * and compliance reporting.
 */
@Service
public class AuditGovernanceService {

    private static final Logger log = LoggerFactory.getLogger(AuditGovernanceService.class);

    private final AuditLogRepositoryPort auditLogRepository;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    public AuditGovernanceService(
            AuditLogRepositoryPort auditLogRepository,
            WorkspaceAccessGuard workspaceAccessGuard
    ) {
        this.auditLogRepository = auditLogRepository;
        this.workspaceAccessGuard = workspaceAccessGuard;
    }

    /**
     * Appends an immutable audit log entry with SHA-256 signature hash.
     */
    public AuditLogEntry logAction(
            UserId userId,
            WorkspaceId workspaceId,
            AuditAction action,
            String targetEntityId,
            String previousStateVersion,
            String newStateVersion,
            String rationale,
            String concurGuardConflictRef
    ) {
        AuditLogEntry entry = AuditLogEntry.record(
                userId != null ? userId : UserId.of("SYSTEM"),
                workspaceId,
                action.name(),
                targetEntityId,
                previousStateVersion != null ? previousStateVersion : "V1",
                newStateVersion != null ? newStateVersion : "V1",
                rationale != null ? rationale : "",
                concurGuardConflictRef != null ? concurGuardConflictRef : "NONE"
        );

        log.info("Governance audit recorded: [Action: {}] on [Target: {}] by [User: {}] (Sig: {})",
                action, targetEntityId, userId, entry.signatureHash().substring(0, 8));

        return auditLogRepository.save(entry);
    }

    /**
     * Retrieves the audit trail for a workspace. Requires AUDIT_VIEW permission.
     */
    public List<AuditLogEntry> getAuditTrail(WorkspaceId workspaceId) {
        if (workspaceAccessGuard != null) {
            workspaceAccessGuard.enforceWorkspaceAndPermission(workspaceId, Permission.AUDIT_VIEW);
        }
        return auditLogRepository.findByWorkspaceId(workspaceId);
    }

    /**
     * Retrieves all audit logs for system administrators.
     */
    public List<AuditLogEntry> getAllAuditLogs() {
        if (workspaceAccessGuard != null) {
            workspaceAccessGuard.enforcePermission(Permission.AUDIT_VIEW);
        }
        return auditLogRepository.findAll();
    }

    /**
     * Verifies the cryptographic integrity of a specific audit log record.
     */
    public boolean verifyIntegrity(AuditLogId id) {
        Optional<AuditLogEntry> entryOpt = auditLogRepository.findById(id);
        if (entryOpt.isEmpty()) {
            return false;
        }
        return entryOpt.get().isSignatureValid();
    }
}

