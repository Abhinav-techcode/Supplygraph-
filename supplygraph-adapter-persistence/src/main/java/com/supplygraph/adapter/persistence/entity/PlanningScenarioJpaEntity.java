package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sg_planning_scenarios")
public class PlanningScenarioJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "scenario_name", length = 255, nullable = false)
    private String scenarioName;

    @Column(name = "scenario_type", length = 64, nullable = false)
    private String scenarioType;

    @Column(name = "status", length = 64, nullable = false)
    private String status;

    @Column(name = "total_projected_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalProjectedCost;

    @Column(name = "customer_service_fill_rate", nullable = false)
    private double customerServiceFillRate;

    @Column(name = "average_lead_time_days", nullable = false)
    private double averageLeadTimeDays;

    @Column(name = "carbon_footprint_co2_tons", nullable = false)
    private double carbonFootprintCo2Tons;

    @Column(name = "evaluated_by", length = 64, nullable = false)
    private String evaluatedBy;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    public PlanningScenarioJpaEntity() {}

    public PlanningScenarioJpaEntity(
            String id,
            String workspaceId,
            String scenarioName,
            String scenarioType,
            String status,
            BigDecimal totalProjectedCost,
            double customerServiceFillRate,
            double averageLeadTimeDays,
            double carbonFootprintCo2Tons,
            String evaluatedBy,
            Instant evaluatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.scenarioName = scenarioName;
        this.scenarioType = scenarioType;
        this.status = status;
        this.totalProjectedCost = totalProjectedCost;
        this.customerServiceFillRate = customerServiceFillRate;
        this.averageLeadTimeDays = averageLeadTimeDays;
        this.carbonFootprintCo2Tons = carbonFootprintCo2Tons;
        this.evaluatedBy = evaluatedBy;
        this.evaluatedAt = evaluatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalProjectedCost() { return totalProjectedCost; }
    public void setTotalProjectedCost(BigDecimal totalProjectedCost) { this.totalProjectedCost = totalProjectedCost; }
    public double getCustomerServiceFillRate() { return customerServiceFillRate; }
    public void setCustomerServiceFillRate(double customerServiceFillRate) { this.customerServiceFillRate = customerServiceFillRate; }
    public double getAverageLeadTimeDays() { return averageLeadTimeDays; }
    public void setAverageLeadTimeDays(double averageLeadTimeDays) { this.averageLeadTimeDays = averageLeadTimeDays; }
    public double getCarbonFootprintCo2Tons() { return carbonFootprintCo2Tons; }
    public void setCarbonFootprintCo2Tons(double carbonFootprintCo2Tons) { this.carbonFootprintCo2Tons = carbonFootprintCo2Tons; }
    public String getEvaluatedBy() { return evaluatedBy; }
    public void setEvaluatedBy(String evaluatedBy) { this.evaluatedBy = evaluatedBy; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}

