package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sg_procurement_plans")
public class ProcurementPlanJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "plan_name", length = 255, nullable = false)
    private String planName;

    @Column(name = "net_requirements_json", columnDefinition = "TEXT", nullable = false)
    private String netRequirementsJson;

    @Column(name = "supplier_allocations_json", columnDefinition = "TEXT", nullable = false)
    private String supplierAllocationsJson;

    @Column(name = "total_projected_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalProjectedCost;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public ProcurementPlanJpaEntity() {}

    public ProcurementPlanJpaEntity(
            String id,
            String workspaceId,
            String planName,
            String netRequirementsJson,
            String supplierAllocationsJson,
            BigDecimal totalProjectedCost,
            Instant generatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.planName = planName;
        this.netRequirementsJson = netRequirementsJson;
        this.supplierAllocationsJson = supplierAllocationsJson;
        this.totalProjectedCost = totalProjectedCost;
        this.generatedAt = generatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getNetRequirementsJson() { return netRequirementsJson; }
    public void setNetRequirementsJson(String netRequirementsJson) { this.netRequirementsJson = netRequirementsJson; }
    public String getSupplierAllocationsJson() { return supplierAllocationsJson; }
    public void setSupplierAllocationsJson(String supplierAllocationsJson) { this.supplierAllocationsJson = supplierAllocationsJson; }
    public BigDecimal getTotalProjectedCost() { return totalProjectedCost; }
    public void setTotalProjectedCost(BigDecimal totalProjectedCost) { this.totalProjectedCost = totalProjectedCost; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}

