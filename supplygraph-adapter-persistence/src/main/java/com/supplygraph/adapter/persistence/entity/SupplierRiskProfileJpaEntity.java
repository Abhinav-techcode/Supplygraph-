package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "supplier_risk_profiles")
public class SupplierRiskProfileJpaEntity {

    @Id
    @Column(name = "id", length = 128)
    private String id; // workspaceId:supplierId

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "supplier_id", length = 64, nullable = false)
    private String supplierId;

    @Column(name = "supplier_name", length = 255, nullable = false)
    private String supplierName;

    @Column(name = "tier", nullable = false)
    private int tier;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "overall_risk_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal overallRiskScore;

    @Column(name = "is_single_source", nullable = false)
    private boolean isSingleSource;

    @Column(name = "downstream_sites_impacted", nullable = false)
    private int downstreamSitesImpacted;

    @Column(name = "alternative_suppliers_count", nullable = false)
    private int alternativeSuppliersCount;

    @Column(name = "qualification_status", length = 64)
    private String qualificationStatus;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SupplierRiskProfileJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(BigDecimal overallRiskScore) { this.overallRiskScore = overallRiskScore; }

    public boolean isSingleSource() { return isSingleSource; }
    public void setSingleSource(boolean singleSource) { isSingleSource = singleSource; }

    public int getDownstreamSitesImpacted() { return downstreamSitesImpacted; }
    public void setDownstreamSitesImpacted(int downstreamSitesImpacted) { this.downstreamSitesImpacted = downstreamSitesImpacted; }

    public int getAlternativeSuppliersCount() { return alternativeSuppliersCount; }
    public void setAlternativeSuppliersCount(int alternativeSuppliersCount) { this.alternativeSuppliersCount = alternativeSuppliersCount; }

    public String getQualificationStatus() { return qualificationStatus; }
    public void setQualificationStatus(String qualificationStatus) { this.qualificationStatus = qualificationStatus; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
