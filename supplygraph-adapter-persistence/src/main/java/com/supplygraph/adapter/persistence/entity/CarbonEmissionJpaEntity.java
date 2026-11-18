package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_carbon_emission_records")
public class CarbonEmissionJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "facility_or_lane_id", length = 64, nullable = false)
    private String facilityOrLaneId;

    @Column(name = "scope", length = 64, nullable = false)
    private String scope;

    @Column(name = "activity_category", length = 255, nullable = false)
    private String activityCategory;

    @Column(name = "activity_amount", nullable = false)
    private double activityAmount;

    @Column(name = "unit_of_measure", length = 32, nullable = false)
    private String unitOfMeasure;

    @Column(name = "emission_factor_kg_co2e", nullable = false)
    private double emissionFactorKgCo2e;

    @Column(name = "total_emissions_kg_co2e", nullable = false)
    private double totalEmissionsKgCo2e;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public CarbonEmissionJpaEntity() {}

    public CarbonEmissionJpaEntity(
            String id,
            String workspaceId,
            String facilityOrLaneId,
            String scope,
            String activityCategory,
            double activityAmount,
            String unitOfMeasure,
            double emissionFactorKgCo2e,
            double totalEmissionsKgCo2e,
            Instant recordedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.facilityOrLaneId = facilityOrLaneId;
        this.scope = scope;
        this.activityCategory = activityCategory;
        this.activityAmount = activityAmount;
        this.unitOfMeasure = unitOfMeasure;
        this.emissionFactorKgCo2e = emissionFactorKgCo2e;
        this.totalEmissionsKgCo2e = totalEmissionsKgCo2e;
        this.recordedAt = recordedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getFacilityOrLaneId() { return facilityOrLaneId; }
    public void setFacilityOrLaneId(String facilityOrLaneId) { this.facilityOrLaneId = facilityOrLaneId; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getActivityCategory() { return activityCategory; }
    public void setActivityCategory(String activityCategory) { this.activityCategory = activityCategory; }
    public double getActivityAmount() { return activityAmount; }
    public void setActivityAmount(double activityAmount) { this.activityAmount = activityAmount; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public double getEmissionFactorKgCo2e() { return emissionFactorKgCo2e; }
    public void setEmissionFactorKgCo2e(double emissionFactorKgCo2e) { this.emissionFactorKgCo2e = emissionFactorKgCo2e; }
    public double getTotalEmissionsKgCo2e() { return totalEmissionsKgCo2e; }
    public void setTotalEmissionsKgCo2e(double totalEmissionsKgCo2e) { this.totalEmissionsKgCo2e = totalEmissionsKgCo2e; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}

