package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_supplier_esg_scorecards")
public class SupplierEsgScorecardJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "supplier_id", length = 64, nullable = false)
    private String supplierId;

    @Column(name = "environmental_score", nullable = false)
    private double environmentalScore;

    @Column(name = "social_score", nullable = false)
    private double socialScore;

    @Column(name = "governance_score", nullable = false)
    private double governanceScore;

    @Column(name = "composite_esg_score", nullable = false)
    private double compositeEsgScore;

    @Column(name = "cbam_compliant", nullable = false)
    private boolean cbamCompliant;

    @Column(name = "conflict_minerals_certified", nullable = false)
    private boolean conflictMineralsCertified;

    @Column(name = "eudr_compliant", nullable = false)
    private boolean eudrCompliant;

    @Column(name = "audited_at", nullable = false)
    private Instant auditedAt;

    public SupplierEsgScorecardJpaEntity() {}

    public SupplierEsgScorecardJpaEntity(
            String id,
            String workspaceId,
            String supplierId,
            double environmentalScore,
            double socialScore,
            double governanceScore,
            double compositeEsgScore,
            boolean cbamCompliant,
            boolean conflictMineralsCertified,
            boolean eudrCompliant,
            Instant auditedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.supplierId = supplierId;
        this.environmentalScore = environmentalScore;
        this.socialScore = socialScore;
        this.governanceScore = governanceScore;
        this.compositeEsgScore = compositeEsgScore;
        this.cbamCompliant = cbamCompliant;
        this.conflictMineralsCertified = conflictMineralsCertified;
        this.eudrCompliant = eudrCompliant;
        this.auditedAt = auditedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public double getEnvironmentalScore() { return environmentalScore; }
    public void setEnvironmentalScore(double environmentalScore) { this.environmentalScore = environmentalScore; }
    public double getSocialScore() { return socialScore; }
    public void setSocialScore(double socialScore) { this.socialScore = socialScore; }
    public double getGovernanceScore() { return governanceScore; }
    public void setGovernanceScore(double governanceScore) { this.governanceScore = governanceScore; }
    public double getCompositeEsgScore() { return compositeEsgScore; }
    public void setCompositeEsgScore(double compositeEsgScore) { this.compositeEsgScore = compositeEsgScore; }
    public boolean isCbamCompliant() { return cbamCompliant; }
    public void setCbamCompliant(boolean cbamCompliant) { this.cbamCompliant = cbamCompliant; }
    public boolean isConflictMineralsCertified() { return conflictMineralsCertified; }
    public void setConflictMineralsCertified(boolean conflictMineralsCertified) { this.conflictMineralsCertified = conflictMineralsCertified; }
    public boolean isEudrCompliant() { return eudrCompliant; }
    public void setEudrCompliant(boolean eudrCompliant) { this.eudrCompliant = eudrCompliant; }
    public Instant getAuditedAt() { return auditedAt; }
    public void setAuditedAt(Instant auditedAt) { this.auditedAt = auditedAt; }
}

