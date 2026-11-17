package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "network_resilience_assessments")
public class NetworkResilienceAssessmentJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @Column(name = "overall_resilience_index", precision = 5, scale = 2, nullable = false)
    private BigDecimal overallResilienceIndex;

    @Column(name = "spof_count", nullable = false)
    private int spofCount;

    @Column(name = "supplier_concentration_hhi", precision = 10, scale = 2, nullable = false)
    private BigDecimal supplierConcentrationHHI;

    @Column(name = "critical_path_depth", nullable = false)
    private int criticalPathDepth;

    @Column(name = "alt_coverage_ratio", precision = 5, scale = 4, nullable = false)
    private BigDecimal altCoverageRatio;

    @Column(name = "bottlenecks_count", nullable = false)
    private int bottlenecksCount;

    public NetworkResilienceAssessmentJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public Instant getEvaluatedAt() { return assessedAt; }
    public void setEvaluatedAt(Instant assessedAt) { this.assessedAt = assessedAt; }

    public BigDecimal getOverallResilienceIndex() { return overallResilienceIndex; }
    public void setOverallResilienceIndex(BigDecimal overallResilienceIndex) { this.overallResilienceIndex = overallResilienceIndex; }

    public int getSpofCount() { return spofCount; }
    public void setSpofCount(int spofCount) { this.spofCount = spofCount; }

    public BigDecimal getSupplierConcentrationHHI() { return supplierConcentrationHHI; }
    public void setSupplierConcentrationHHI(BigDecimal supplierConcentrationHHI) { this.supplierConcentrationHHI = supplierConcentrationHHI; }

    public int getCriticalPathDepth() { return criticalPathDepth; }
    public void setCriticalPathDepth(int criticalPathDepth) { this.criticalPathDepth = criticalPathDepth; }

    public BigDecimal getAltCoverageRatio() { return altCoverageRatio; }
    public void setAltCoverageRatio(BigDecimal altCoverageRatio) { this.altCoverageRatio = altCoverageRatio; }

    public int getBottlenecksCount() { return bottlenecksCount; }
    public void setBottlenecksCount(int bottlenecksCount) { this.bottlenecksCount = bottlenecksCount; }
}
