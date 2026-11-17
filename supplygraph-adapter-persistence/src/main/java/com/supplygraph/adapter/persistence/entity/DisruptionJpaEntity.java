package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "disruptions")
public class DisruptionJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "industry_code", length = 64, nullable = false)
    private String industryCode;

    @Column(name = "scenario_type", length = 64, nullable = false)
    private String scenarioType;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 64, nullable = false)
    private String category;

    @Column(name = "severity", length = 32, nullable = false)
    private String severity;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "affected_nodes_json", columnDefinition = "TEXT")
    private String affectedNodesJson;

    @Column(name = "affected_lanes_json", columnDefinition = "TEXT")
    private String affectedLanesJson;

    @Column(name = "affected_materials_json", columnDefinition = "TEXT")
    private String affectedMaterialsJson;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "expected_end_time")
    private Instant expectedEndTime;

    @Column(name = "impact_params_json", columnDefinition = "TEXT")
    private String impactParamsJson;

    @Column(name = "source_evidence", columnDefinition = "TEXT")
    private String sourceEvidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version", nullable = false)
    private Long version;

    protected DisruptionJpaEntity() {
    }

    public DisruptionJpaEntity(
            String id, String workspaceId, String industryCode, String scenarioType,
            String title, String description, String category, String severity, String status,
            String affectedNodesJson, String affectedLanesJson, String affectedMaterialsJson,
            Instant startTime, Instant expectedEndTime, String impactParamsJson, String sourceEvidence,
            Instant createdAt, Instant updatedAt, Long version
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.industryCode = Objects.requireNonNull(industryCode, "industryCode must not be null");
        this.scenarioType = Objects.requireNonNull(scenarioType, "scenarioType must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.affectedNodesJson = affectedNodesJson;
        this.affectedLanesJson = affectedLanesJson;
        this.affectedMaterialsJson = affectedMaterialsJson;
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.expectedEndTime = expectedEndTime;
        this.impactParamsJson = impactParamsJson;
        this.sourceEvidence = sourceEvidence;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version == null ? 0L : version;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getIndustryCode() { return industryCode; }
    public String getScenarioType() { return scenarioType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public String getAffectedNodesJson() { return affectedNodesJson; }
    public String getAffectedLanesJson() { return affectedLanesJson; }
    public String getAffectedMaterialsJson() { return affectedMaterialsJson; }
    public Instant getStartTime() { return startTime; }
    public Instant getExpectedEndTime() { return expectedEndTime; }
    public String getImpactParamsJson() { return impactParamsJson; }
    public String getSourceEvidence() { return sourceEvidence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setStatus(String status) { this.status = status; }
    public void setExpectedEndTime(Instant expectedEndTime) { this.expectedEndTime = expectedEndTime; }
    public void setImpactParamsJson(String impactParamsJson) { this.impactParamsJson = impactParamsJson; }
    public void setSourceEvidence(String sourceEvidence) { this.sourceEvidence = sourceEvidence; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }
}
