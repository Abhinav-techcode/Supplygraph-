package com.supplygraph.domain.model.monitoring;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.twin.StateVersion;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Actionable operational risk finding produced by the monitoring engine.
 * Retains strict provenance, digital twin state version, and factual evidence.
 */
public class RiskAlert {

    private final RiskAlertId id;
    private final WorkspaceId workspaceId;
    private final MonitoringRuleId ruleId;
    private final MonitoringSeverity severity;
    private final String title;
    private final String description;
    private final String affectedEntityId;
    private final String affectedEntityType;
    private final EventId triggerEventId;
    private final StateVersion sourceStateVersion;
    private final Map<String, String> evidence;
    private final String suppressionKey;
    private RiskAlertStatus status;
    private final Instant detectedAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;

    public RiskAlert(
            RiskAlertId id,
            WorkspaceId workspaceId,
            MonitoringRuleId ruleId,
            MonitoringSeverity severity,
            String title,
            String description,
            String affectedEntityId,
            String affectedEntityType,
            EventId triggerEventId,
            StateVersion sourceStateVersion,
            Map<String, String> evidence,
            String suppressionKey,
            RiskAlertStatus status,
            Instant detectedAt,
            Instant acknowledgedAt,
            Instant resolvedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.affectedEntityId = Objects.requireNonNull(affectedEntityId, "affectedEntityId must not be null");
        this.affectedEntityType = Objects.requireNonNull(affectedEntityType, "affectedEntityType must not be null");
        this.triggerEventId = triggerEventId;
        this.sourceStateVersion = sourceStateVersion != null ? sourceStateVersion : StateVersion.initial();
        this.evidence = evidence != null ? Map.copyOf(evidence) : Collections.emptyMap();
        this.suppressionKey = suppressionKey != null ? suppressionKey : (workspaceId.value() + ":" + ruleId.value() + ":" + affectedEntityId);
        this.status = status != null ? status : RiskAlertStatus.ACTIVE;
        this.detectedAt = detectedAt != null ? detectedAt : Instant.now();
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
    }

    public static RiskAlert createActive(
            WorkspaceId workspaceId,
            MonitoringRuleId ruleId,
            MonitoringSeverity severity,
            String title,
            String description,
            String affectedEntityId,
            String affectedEntityType,
            EventId triggerEventId,
            StateVersion sourceStateVersion,
            Map<String, String> evidence
    ) {
        String suppressionKey = workspaceId.value() + ":" + ruleId.value() + ":" + affectedEntityId;
        return new RiskAlert(
                RiskAlertId.generate(),
                workspaceId,
                ruleId,
                severity,
                title,
                description,
                affectedEntityId,
                affectedEntityType,
                triggerEventId,
                sourceStateVersion,
                evidence,
                suppressionKey,
                RiskAlertStatus.ACTIVE,
                Instant.now(),
                null,
                null
        );
    }

    public void acknowledge(Instant when) {
        this.status = RiskAlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = when != null ? when : Instant.now();
    }

    public void resolve(Instant when) {
        this.status = RiskAlertStatus.RESOLVED;
        this.resolvedAt = when != null ? when : Instant.now();
    }

    public void suppress() {
        this.status = RiskAlertStatus.SUPPRESSED;
    }

    public RiskAlertId getId() {
        return id;
    }

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public MonitoringRuleId getRuleId() {
        return ruleId;
    }

    public MonitoringSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAffectedEntityId() {
        return affectedEntityId;
    }

    public String getAffectedEntityType() {
        return affectedEntityType;
    }

    public EventId getTriggerEventId() {
        return triggerEventId;
    }

    public StateVersion getSourceStateVersion() {
        return sourceStateVersion;
    }

    public Map<String, String> getEvidence() {
        return evidence;
    }

    public String getSuppressionKey() {
        return suppressionKey;
    }

    public RiskAlertStatus getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}

