package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "impact_reassessments")
public class ImpactReassessmentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "trigger_event_id", length = 64)
    private String triggerEventId;

    @Column(name = "previous_version", nullable = false)
    private Long previousVersion;

    @Column(name = "new_version", nullable = false)
    private Long newVersion;

    @Column(name = "affected_node_ids", columnDefinition = "TEXT", nullable = false)
    private String affectedNodeIds;

    @Column(name = "affected_lane_ids", columnDefinition = "TEXT", nullable = false)
    private String affectedLaneIds;

    @Column(name = "affected_material_ids", columnDefinition = "TEXT", nullable = false)
    private String affectedMaterialIds;

    @Column(name = "preserved_scope_json", columnDefinition = "TEXT", nullable = false)
    private String preservedScopeJson;

    @Column(name = "invalidated_scope_json", columnDefinition = "TEXT", nullable = false)
    private String invalidatedScopeJson;

    @Column(name = "affected_assessment_ids", columnDefinition = "TEXT", nullable = false)
    private String affectedAssessmentIds;

    @Column(name = "affected_plan_ids", columnDefinition = "TEXT", nullable = false)
    private String affectedPlanIds;

    @Column(name = "reassessed_at", nullable = false)
    private Instant reassessedAt;

    protected ImpactReassessmentJpaEntity() {}

    public ImpactReassessmentJpaEntity(
            String id, String workspaceId, String triggerEventId,
            Long previousVersion, Long newVersion, String affectedNodeIds,
            String affectedLaneIds, String affectedMaterialIds,
            String preservedScopeJson, String invalidatedScopeJson,
            String affectedAssessmentIds, String affectedPlanIds, Instant reassessedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.triggerEventId = triggerEventId;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.affectedNodeIds = affectedNodeIds;
        this.affectedLaneIds = affectedLaneIds;
        this.affectedMaterialIds = affectedMaterialIds;
        this.preservedScopeJson = preservedScopeJson;
        this.invalidatedScopeJson = invalidatedScopeJson;
        this.affectedAssessmentIds = affectedAssessmentIds;
        this.affectedPlanIds = affectedPlanIds;
        this.reassessedAt = reassessedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getTriggerEventId() { return triggerEventId; }
    public Long getPreviousVersion() { return previousVersion; }
    public Long getNewVersion() { return newVersion; }
    public String getAffectedNodeIds() { return affectedNodeIds; }
    public String getAffectedLaneIds() { return affectedLaneIds; }
    public String getAffectedMaterialIds() { return affectedMaterialIds; }
    public String getPreservedScopeJson() { return preservedScopeJson; }
    public String getInvalidatedScopeJson() { return invalidatedScopeJson; }
    public String getAffectedAssessmentIds() { return affectedAssessmentIds; }
    public String getAffectedPlanIds() { return affectedPlanIds; }
    public Instant getReassessedAt() { return reassessedAt; }
}

