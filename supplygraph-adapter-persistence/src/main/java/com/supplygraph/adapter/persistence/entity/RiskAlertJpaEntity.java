package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "risk_alerts")
public class RiskAlertJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "rule_id", length = 64, nullable = false)
    private String ruleId;

    @Column(name = "severity", length = 32, nullable = false)
    private String severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "affected_entity_id", length = 128, nullable = false)
    private String affectedEntityId;

    @Column(name = "affected_entity_type", length = 64, nullable = false)
    private String affectedEntityType;

    @Column(name = "trigger_event_id", length = 64)
    private String triggerEventId;

    @Column(name = "source_state_version", nullable = false)
    private Long sourceStateVersion;

    @Column(name = "evidence_json", columnDefinition = "TEXT", nullable = false)
    private String evidenceJson;

    @Column(name = "suppression_key", nullable = false)
    private String suppressionKey;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected RiskAlertJpaEntity() {}

    public RiskAlertJpaEntity(
            String id, String workspaceId, String ruleId, String severity,
            String title, String description, String affectedEntityId, String affectedEntityType,
            String triggerEventId, Long sourceStateVersion, String evidenceJson, String suppressionKey,
            String status, Instant detectedAt, Instant acknowledgedAt, Instant resolvedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.ruleId = ruleId;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.affectedEntityId = affectedEntityId;
        this.affectedEntityType = affectedEntityType;
        this.triggerEventId = triggerEventId;
        this.sourceStateVersion = sourceStateVersion;
        this.evidenceJson = evidenceJson;
        this.suppressionKey = suppressionKey;
        this.status = status;
        this.detectedAt = detectedAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getRuleId() { return ruleId; }
    public String getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAffectedEntityId() { return affectedEntityId; }
    public String getAffectedEntityType() { return affectedEntityType; }
    public String getTriggerEventId() { return triggerEventId; }
    public Long getSourceStateVersion() { return sourceStateVersion; }
    public String getEvidenceJson() { return evidenceJson; }
    public String getSuppressionKey() { return suppressionKey; }
    public String getStatus() { return status; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}

