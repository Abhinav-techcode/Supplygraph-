package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_circular_ledger_entries")
public class CircularLedgerJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "flow_type", length = 64, nullable = false)
    private String flowType;

    @Column(name = "intake_quantity", nullable = false)
    private double intakeQuantity;

    @Column(name = "recovery_yield_percentage", nullable = false)
    private double recoveryYieldPercentage;

    @Column(name = "recovered_quantity", nullable = false)
    private double recoveredQuantity;

    @Column(name = "virgin_material_avoided_kg", nullable = false)
    private double virginMaterialAvoidedKg;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public CircularLedgerJpaEntity() {}

    public CircularLedgerJpaEntity(
            String id,
            String workspaceId,
            String materialId,
            String flowType,
            double intakeQuantity,
            double recoveryYieldPercentage,
            double recoveredQuantity,
            double virginMaterialAvoidedKg,
            Instant recordedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.materialId = materialId;
        this.flowType = flowType;
        this.intakeQuantity = intakeQuantity;
        this.recoveryYieldPercentage = recoveryYieldPercentage;
        this.recoveredQuantity = recoveredQuantity;
        this.virginMaterialAvoidedKg = virginMaterialAvoidedKg;
        this.recordedAt = recordedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }
    public double getIntakeQuantity() { return intakeQuantity; }
    public void setIntakeQuantity(double intakeQuantity) { this.intakeQuantity = intakeQuantity; }
    public double getRecoveryYieldPercentage() { return recoveryYieldPercentage; }
    public void setRecoveryYieldPercentage(double recoveryYieldPercentage) { this.recoveryYieldPercentage = recoveryYieldPercentage; }
    public double getRecoveredQuantity() { return recoveredQuantity; }
    public void setRecoveredQuantity(double recoveredQuantity) { this.recoveredQuantity = recoveredQuantity; }
    public double getVirginMaterialAvoidedKg() { return virginMaterialAvoidedKg; }
    public void setVirginMaterialAvoidedKg(double virginMaterialAvoidedKg) { this.virginMaterialAvoidedKg = virginMaterialAvoidedKg; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}

