package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Column(name = "target_entity_id", length = 128, nullable = false)
    private String targetEntityId;

    @Column(name = "previous_state_version", length = 32)
    private String previousStateVersion;

    @Column(name = "new_state_version", length = 32)
    private String newStateVersion;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "concurguard_conflict_ref", length = 128)
    private String concurGuardConflictRef;

    @Column(name = "signature_hash", length = 128, nullable = false)
    private String signatureHash;

    protected AuditLogJpaEntity() {}

    public AuditLogJpaEntity(
            String id,
            String userId,
            String workspaceId,
            String action,
            String targetEntityId,
            String previousStateVersion,
            String newStateVersion,
            Instant timestamp,
            String rationale,
            String concurGuardConflictRef,
            String signatureHash
    ) {
        this.id = id;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.action = action;
        this.targetEntityId = targetEntityId;
        this.previousStateVersion = previousStateVersion;
        this.newStateVersion = newStateVersion;
        this.timestamp = timestamp;
        this.rationale = rationale;
        this.concurGuardConflictRef = concurGuardConflictRef;
        this.signatureHash = signatureHash;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getWorkspaceId() { return workspaceId; }
    public String getAction() { return action; }
    public String getTargetEntityId() { return targetEntityId; }
    public String getPreviousStateVersion() { return previousStateVersion; }
    public String getNewStateVersion() { return newStateVersion; }
    public Instant getTimestamp() { return timestamp; }
    public String getRationale() { return rationale; }
    public String getConcurGuardConflictRef() { return concurGuardConflictRef; }
    public String getSignatureHash() { return signatureHash; }
}

