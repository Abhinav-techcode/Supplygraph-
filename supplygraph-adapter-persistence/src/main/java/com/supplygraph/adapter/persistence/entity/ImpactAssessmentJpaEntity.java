package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "impact_assessments")
public class ImpactAssessmentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "digital_twin_version", nullable = false)
    private Long digitalTwinVersion;

    @Column(name = "assessment_timestamp", nullable = false)
    private Instant assessmentTimestamp;

    @Column(name = "overall_severity", length = 32, nullable = false)
    private String overallSeverity;

    @Column(name = "node_impacts_json", columnDefinition = "TEXT")
    private String nodeImpactsJson;

    @Column(name = "material_shortages_json", columnDefinition = "TEXT")
    private String materialShortagesJson;

    @Column(name = "capacity_impacts_json", columnDefinition = "TEXT")
    private String capacityImpactsJson;

    @Column(name = "logistics_impacts_json", columnDefinition = "TEXT")
    private String logisticsImpactsJson;

    @Column(name = "total_estimated_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalEstimatedCost;

    @Column(name = "risk_indicators_json", columnDefinition = "TEXT")
    private String riskIndicatorsJson;

    @Column(name = "assumptions_json", columnDefinition = "TEXT")
    private String assumptionsJson;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "version", nullable = false)
    private Long version;

    protected ImpactAssessmentJpaEntity() {
    }

    public ImpactAssessmentJpaEntity(
            String id, String disruptionId, String workspaceId, Long digitalTwinVersion,
            Instant assessmentTimestamp, String overallSeverity, String nodeImpactsJson,
            String materialShortagesJson, String capacityImpactsJson, String logisticsImpactsJson,
            BigDecimal totalEstimatedCost, String riskIndicatorsJson, String assumptionsJson,
            String status, Long version
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.disruptionId = Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.digitalTwinVersion = Objects.requireNonNull(digitalTwinVersion, "digitalTwinVersion must not be null");
        this.assessmentTimestamp = Objects.requireNonNull(assessmentTimestamp, "assessmentTimestamp must not be null");
        this.overallSeverity = Objects.requireNonNull(overallSeverity, "overallSeverity must not be null");
        this.nodeImpactsJson = nodeImpactsJson;
        this.materialShortagesJson = materialShortagesJson;
        this.capacityImpactsJson = capacityImpactsJson;
        this.logisticsImpactsJson = logisticsImpactsJson;
        this.totalEstimatedCost = totalEstimatedCost == null ? BigDecimal.ZERO : totalEstimatedCost;
        this.riskIndicatorsJson = riskIndicatorsJson;
        this.assumptionsJson = assumptionsJson;
        this.status = status == null ? "COMPLETED" : status;
        this.version = version == null ? 0L : version;
    }

    public String getId() { return id; }
    public String getDisruptionId() { return disruptionId; }
    public String getWorkspaceId() { return workspaceId; }
    public Long getDigitalTwinVersion() { return digitalTwinVersion; }
    public Instant getAssessmentTimestamp() { return assessmentTimestamp; }
    public String getOverallSeverity() { return overallSeverity; }
    public String getNodeImpactsJson() { return nodeImpactsJson; }
    public String getMaterialShortagesJson() { return materialShortagesJson; }
    public String getCapacityImpactsJson() { return capacityImpactsJson; }
    public String getLogisticsImpactsJson() { return logisticsImpactsJson; }
    public BigDecimal getTotalEstimatedCost() { return totalEstimatedCost; }
    public String getRiskIndicatorsJson() { return riskIndicatorsJson; }
    public String getAssumptionsJson() { return assumptionsJson; }
    public String getStatus() { return status; }
    public Long getVersion() { return version; }
}
