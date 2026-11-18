package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sg_environmental_scenarios")
public class EnvironmentalScenarioJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "scenario_name", length = 255, nullable = false)
    private String scenarioName;

    @Column(name = "baseline_co2_tons", nullable = false)
    private double baselineCo2Tons;

    @Column(name = "projected_co2_tons", nullable = false)
    private double projectedCo2Tons;

    @Column(name = "reduction_percentage", nullable = false)
    private double reductionPercentage;

    @Column(name = "total_abatement_cost_usd", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAbatementCostUsd;

    @Column(name = "cost_per_ton_co2_avoided_usd", precision = 19, scale = 4, nullable = false)
    private BigDecimal costPerTonCo2AvoidedUsd;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    public EnvironmentalScenarioJpaEntity() {}

    public EnvironmentalScenarioJpaEntity(
            String id,
            String workspaceId,
            String scenarioName,
            double baselineCo2Tons,
            double projectedCo2Tons,
            double reductionPercentage,
            BigDecimal totalAbatementCostUsd,
            BigDecimal costPerTonCo2AvoidedUsd,
            Instant evaluatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.scenarioName = scenarioName;
        this.baselineCo2Tons = baselineCo2Tons;
        this.projectedCo2Tons = projectedCo2Tons;
        this.reductionPercentage = reductionPercentage;
        this.totalAbatementCostUsd = totalAbatementCostUsd;
        this.costPerTonCo2AvoidedUsd = costPerTonCo2AvoidedUsd;
        this.evaluatedAt = evaluatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public double getBaselineCo2Tons() { return baselineCo2Tons; }
    public void setBaselineCo2Tons(double baselineCo2Tons) { this.baselineCo2Tons = baselineCo2Tons; }
    public double getProjectedCo2Tons() { return projectedCo2Tons; }
    public void setProjectedCo2Tons(double projectedCo2Tons) { this.projectedCo2Tons = projectedCo2Tons; }
    public double getReductionPercentage() { return reductionPercentage; }
    public void setReductionPercentage(double reductionPercentage) { this.reductionPercentage = reductionPercentage; }
    public BigDecimal getTotalAbatementCostUsd() { return totalAbatementCostUsd; }
    public void setTotalAbatementCostUsd(BigDecimal totalAbatementCostUsd) { this.totalAbatementCostUsd = totalAbatementCostUsd; }
    public BigDecimal getCostPerTonCo2AvoidedUsd() { return costPerTonCo2AvoidedUsd; }
    public void setCostPerTonCo2AvoidedUsd(BigDecimal costPerTonCo2AvoidedUsd) { this.costPerTonCo2AvoidedUsd = costPerTonCo2AvoidedUsd; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}

