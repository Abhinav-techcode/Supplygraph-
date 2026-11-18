package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "material_intelligence_profiles")
public class MaterialIntelligenceProfileJpaEntity {

    @Id
    @Column(name = "id", length = 128)
    private String id; // workspaceId:materialId

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "material_name", length = 255, nullable = false)
    private String materialName;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "criticality", length = 32, nullable = false)
    private String criticality;

    @Column(name = "single_source_supplier_id", length = 64)
    private String singleSourceSupplierId;

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Column(name = "on_hand_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal onHandQuantity;

    @Column(name = "available_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal availableQuantity;

    @Column(name = "reserved_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal reservedQuantity;

    @Column(name = "quarantined_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quarantinedQuantity;

    @Column(name = "in_transit_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal inTransitQuantity;

    @Column(name = "daily_consumption_rate", precision = 19, scale = 4, nullable = false)
    private BigDecimal dailyConsumptionRate;

    @Column(name = "inventory_runway_days", precision = 10, scale = 2, nullable = false)
    private BigDecimal inventoryRunwayDays;

    @Column(name = "expiry_risk_batch_count", nullable = false)
    private int expiryRiskBatchCount;

    @Column(name = "downstream_product_count", nullable = false)
    private int downstreamProductCount;

    @Column(name = "industry_classification", length = 64)
    private String industryClassification;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MaterialIntelligenceProfileJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCriticality() { return criticality; }
    public void setCriticality(String criticality) { this.criticality = criticality; }

    public String getSingleSourceSupplierId() { return singleSourceSupplierId; }
    public void setSingleSourceSupplierId(String singleSourceSupplierId) { this.singleSourceSupplierId = singleSourceSupplierId; }

    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    public BigDecimal getOnHandQuantity() { return onHandQuantity; }
    public void setOnHandQuantity(BigDecimal onHandQuantity) { this.onHandQuantity = onHandQuantity; }

    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }

    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(BigDecimal reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public BigDecimal getQuarantinedQuantity() { return quarantinedQuantity; }
    public void setQuarantinedQuantity(BigDecimal quarantinedQuantity) { this.quarantinedQuantity = quarantinedQuantity; }

    public BigDecimal getInTransitQuantity() { return inTransitQuantity; }
    public void setInTransitQuantity(BigDecimal inTransitQuantity) { this.inTransitQuantity = inTransitQuantity; }

    public BigDecimal getDailyConsumptionRate() { return dailyConsumptionRate; }
    public void setDailyConsumptionRate(BigDecimal dailyConsumptionRate) { this.dailyConsumptionRate = dailyConsumptionRate; }

    public BigDecimal getInventoryRunwayDays() { return inventoryRunwayDays; }
    public void setInventoryRunwayDays(BigDecimal inventoryRunwayDays) { this.inventoryRunwayDays = inventoryRunwayDays; }

    public int getExpiryRiskBatchCount() { return expiryRiskBatchCount; }
    public void setExpiryRiskBatchCount(int expiryRiskBatchCount) { this.expiryRiskBatchCount = expiryRiskBatchCount; }

    public int getDownstreamProductCount() { return downstreamProductCount; }
    public void setDownstreamProductCount(int downstreamProductCount) { this.downstreamProductCount = downstreamProductCount; }

    public String getIndustryClassification() { return industryClassification; }
    public void setIndustryClassification(String industryClassification) { this.industryClassification = industryClassification; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

