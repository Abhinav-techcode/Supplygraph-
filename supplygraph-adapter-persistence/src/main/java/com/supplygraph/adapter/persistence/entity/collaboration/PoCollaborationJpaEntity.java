package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sg_po_collaboration_records")
public class PoCollaborationJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "po_number", length = 64, nullable = false)
    private String poNumber;

    @Column(name = "supplier_id", length = 64, nullable = false)
    private String supplierId;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "order_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal orderQuantity;

    @Column(name = "total_amount_usd", precision = 18, scale = 4, nullable = false)
    private BigDecimal totalAmountUsd;

    @Column(name = "promised_delivery_date", nullable = false)
    private LocalDate promisedDeliveryDate;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public PoCollaborationJpaEntity() {}

    public PoCollaborationJpaEntity(String id, String workspaceId, String poNumber, String supplierId, String materialId, BigDecimal orderQuantity, BigDecimal totalAmountUsd, LocalDate promisedDeliveryDate, String status, Instant lastUpdated) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.materialId = materialId;
        this.orderQuantity = orderQuantity;
        this.totalAmountUsd = totalAmountUsd;
        this.promisedDeliveryDate = promisedDeliveryDate;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }

    public BigDecimal getOrderQuantity() { return orderQuantity; }
    public void setOrderQuantity(BigDecimal orderQuantity) { this.orderQuantity = orderQuantity; }

    public BigDecimal getTotalAmountUsd() { return totalAmountUsd; }
    public void setTotalAmountUsd(BigDecimal totalAmountUsd) { this.totalAmountUsd = totalAmountUsd; }

    public LocalDate getPromisedDeliveryDate() { return promisedDeliveryDate; }
    public void setPromisedDeliveryDate(LocalDate promisedDeliveryDate) { this.promisedDeliveryDate = promisedDeliveryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}

