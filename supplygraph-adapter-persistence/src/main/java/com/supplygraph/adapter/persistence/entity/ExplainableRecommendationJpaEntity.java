package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "explainable_recommendations")
public class ExplainableRecommendationJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "state_version", length = 32, nullable = false)
    private String stateVersion;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "primary_actions", columnDefinition = "TEXT", nullable = false)
    private String primaryActions;

    @Column(name = "expected_cost_usd", precision = 19, scale = 4, nullable = false)
    private BigDecimal expectedCostUsd;

    @Column(name = "expected_recovery_time_hours", nullable = false)
    private double expectedRecoveryTimeHours;

    @Column(name = "service_restoration_pct", nullable = false)
    private double serviceRestorationPct;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "constraints_json", columnDefinition = "TEXT")
    private String constraintsJson;

    @Column(name = "tradeoffs_json", columnDefinition = "TEXT")
    private String tradeoffsJson;

    @Column(name = "uncertainty_json", columnDefinition = "TEXT")
    private String uncertaintyJson;

    @Column(name = "requires_human_approval", nullable = false)
    private boolean requiresHumanApproval;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "approved_by", length = 64)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    protected ExplainableRecommendationJpaEntity() {}

    public ExplainableRecommendationJpaEntity(
            String id, String disruptionId, String stateVersion, String title, String summary,
            String primaryActions, BigDecimal expectedCostUsd, double expectedRecoveryTimeHours,
            double serviceRestorationPct, String evidenceJson, String constraintsJson,
            String tradeoffsJson, String uncertaintyJson, boolean requiresHumanApproval,
            boolean approved, String approvedBy, Instant approvedAt, String rejectionReason
    ) {
        this.id = id;
        this.disruptionId = disruptionId;
        this.stateVersion = stateVersion;
        this.title = title;
        this.summary = summary;
        this.primaryActions = primaryActions;
        this.expectedCostUsd = expectedCostUsd;
        this.expectedRecoveryTimeHours = expectedRecoveryTimeHours;
        this.serviceRestorationPct = serviceRestorationPct;
        this.evidenceJson = evidenceJson;
        this.constraintsJson = constraintsJson;
        this.tradeoffsJson = tradeoffsJson;
        this.uncertaintyJson = uncertaintyJson;
        this.requiresHumanApproval = requiresHumanApproval;
        this.approved = approved;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
    }

    public String getId() { return id; }
    public String getDisruptionId() { return disruptionId; }
    public String getStateVersion() { return stateVersion; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getPrimaryActions() { return primaryActions; }
    public BigDecimal getExpectedCostUsd() { return expectedCostUsd; }
    public double getExpectedRecoveryTimeHours() { return expectedRecoveryTimeHours; }
    public double getServiceRestorationPct() { return serviceRestorationPct; }
    public String getEvidenceJson() { return evidenceJson; }
    public String getConstraintsJson() { return constraintsJson; }
    public String getTradeoffsJson() { return tradeoffsJson; }
    public String getUncertaintyJson() { return uncertaintyJson; }
    public boolean isRequiresHumanApproval() { return requiresHumanApproval; }
    public boolean isApproved() { return approved; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
}
