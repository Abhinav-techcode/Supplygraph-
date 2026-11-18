package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_inventory_plans")
public class InventoryPlanJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "plan_name", length = 255, nullable = false)
    private String planName;

    @Column(name = "target_service_level", length = 32, nullable = false)
    private String targetServiceLevel;

    @Column(name = "positions_json", columnDefinition = "TEXT", nullable = false)
    private String positionsJson;

    @Column(name = "total_carrying_cost", nullable = false)
    private double totalCarryingCost;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public InventoryPlanJpaEntity() {}

    public InventoryPlanJpaEntity(
            String id,
            String workspaceId,
            String planName,
            String targetServiceLevel,
            String positionsJson,
            double totalCarryingCost,
            Instant calculatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.planName = planName;
        this.targetServiceLevel = targetServiceLevel;
        this.positionsJson = positionsJson;
        this.totalCarryingCost = totalCarryingCost;
        this.calculatedAt = calculatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getTargetServiceLevel() { return targetServiceLevel; }
    public void setTargetServiceLevel(String targetServiceLevel) { this.targetServiceLevel = targetServiceLevel; }
    public String getPositionsJson() { return positionsJson; }
    public void setPositionsJson(String positionsJson) { this.positionsJson = positionsJson; }
    public double getTotalCarryingCost() { return totalCarryingCost; }
    public void setTotalCarryingCost(double totalCarryingCost) { this.totalCarryingCost = totalCarryingCost; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}

