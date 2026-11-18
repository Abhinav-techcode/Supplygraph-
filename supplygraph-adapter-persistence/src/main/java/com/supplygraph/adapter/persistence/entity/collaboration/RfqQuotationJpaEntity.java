package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sg_rfq_quotations")
public class RfqQuotationJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "rfq_id", length = 64, nullable = false)
    private String rfqId;

    @Column(name = "supplier_id", length = 64, nullable = false)
    private String supplierId;

    @Column(name = "unit_price_usd", precision = 18, scale = 4, nullable = false)
    private BigDecimal unitPriceUsd;

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Column(name = "min_order_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal minOrderQuantity;

    @Column(name = "validity_date", nullable = false)
    private LocalDate validityDate;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public RfqQuotationJpaEntity() {}

    public RfqQuotationJpaEntity(String id, String rfqId, String supplierId, BigDecimal unitPriceUsd, int leadTimeDays, BigDecimal minOrderQuantity, LocalDate validityDate, String status, Instant submittedAt) {
        this.id = id;
        this.rfqId = rfqId;
        this.supplierId = supplierId;
        this.unitPriceUsd = unitPriceUsd;
        this.leadTimeDays = leadTimeDays;
        this.minOrderQuantity = minOrderQuantity;
        this.validityDate = validityDate;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRfqId() { return rfqId; }
    public void setRfqId(String rfqId) { this.rfqId = rfqId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public BigDecimal getUnitPriceUsd() { return unitPriceUsd; }
    public void setUnitPriceUsd(BigDecimal unitPriceUsd) { this.unitPriceUsd = unitPriceUsd; }

    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    public BigDecimal getMinOrderQuantity() { return minOrderQuantity; }
    public void setMinOrderQuantity(BigDecimal minOrderQuantity) { this.minOrderQuantity = minOrderQuantity; }

    public LocalDate getValidityDate() { return validityDate; }
    public void setValidityDate(LocalDate validityDate) { this.validityDate = validityDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}

