package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sg_advance_shipping_notices")
public class AdvanceShippingNoticeJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "po_id", length = 64, nullable = false)
    private String poId;

    @Column(name = "tracking_number", length = 128, nullable = false)
    private String trackingNumber;

    @Column(name = "carrier_name", length = 128, nullable = false)
    private String carrierName;

    @Column(name = "shipped_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal shippedQuantity;

    @Column(name = "estimated_arrival_date", nullable = false)
    private LocalDate estimatedArrivalDate;

    @Column(name = "dispatched_at", nullable = false)
    private Instant dispatchedAt;

    public AdvanceShippingNoticeJpaEntity() {}

    public AdvanceShippingNoticeJpaEntity(String id, String workspaceId, String poId, String trackingNumber, String carrierName, BigDecimal shippedQuantity, LocalDate estimatedArrivalDate, Instant dispatchedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.poId = poId;
        this.trackingNumber = trackingNumber;
        this.carrierName = carrierName;
        this.shippedQuantity = shippedQuantity;
        this.estimatedArrivalDate = estimatedArrivalDate;
        this.dispatchedAt = dispatchedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getPoId() { return poId; }
    public void setPoId(String poId) { this.poId = poId; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

    public BigDecimal getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(BigDecimal shippedQuantity) { this.shippedQuantity = shippedQuantity; }

    public LocalDate getEstimatedArrivalDate() { return estimatedArrivalDate; }
    public void setEstimatedArrivalDate(LocalDate estimatedArrivalDate) { this.estimatedArrivalDate = estimatedArrivalDate; }

    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }
}

