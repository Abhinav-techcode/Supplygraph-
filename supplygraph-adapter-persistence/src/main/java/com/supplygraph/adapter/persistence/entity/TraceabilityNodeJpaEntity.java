package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_traceability_nodes")
public class TraceabilityNodeJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "batch_number", length = 128, nullable = false)
    private String batchNumber;

    @Column(name = "node_type", length = 64, nullable = false)
    private String nodeType;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "location_id", length = 64, nullable = false)
    private String locationId;

    @Column(name = "quantity", nullable = false)
    private double quantity;

    @Column(name = "unit_of_measure", length = 32, nullable = false)
    private String unitOfMeasure;

    @Column(name = "custody_holder", length = 255, nullable = false)
    private String custodyHolder;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public TraceabilityNodeJpaEntity() {}

    public TraceabilityNodeJpaEntity(
            String id,
            String workspaceId,
            String batchNumber,
            String nodeType,
            String materialId,
            String locationId,
            double quantity,
            String unitOfMeasure,
            String custodyHolder,
            Instant recordedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.batchNumber = batchNumber;
        this.nodeType = nodeType;
        this.materialId = materialId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.custodyHolder = custodyHolder;
        this.recordedAt = recordedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public String getCustodyHolder() { return custodyHolder; }
    public void setCustodyHolder(String custodyHolder) { this.custodyHolder = custodyHolder; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}

