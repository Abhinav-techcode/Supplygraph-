package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_demand_forecasts")
public class DemandForecastJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "destination_node_id", length = 64, nullable = false)
    private String destinationNodeId;

    @Column(name = "method", length = 64, nullable = false)
    private String method;

    @Column(name = "forecasted_quantities_json", columnDefinition = "TEXT", nullable = false)
    private String forecastedQuantitiesJson;

    @Column(name = "historical_baseline_json", columnDefinition = "TEXT", nullable = false)
    private String historicalBaselineJson;

    @Column(name = "period_overrides_json", columnDefinition = "TEXT")
    private String periodOverridesJson;

    @Column(name = "mad", nullable = false)
    private double mad;

    @Column(name = "mape", nullable = false)
    private double mape;

    @Column(name = "rmse", nullable = false)
    private double rmse;

    @Column(name = "tracking_signal", nullable = false)
    private double trackingSignal;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public DemandForecastJpaEntity() {}

    public DemandForecastJpaEntity(
            String id,
            String workspaceId,
            String materialId,
            String destinationNodeId,
            String method,
            String forecastedQuantitiesJson,
            String historicalBaselineJson,
            String periodOverridesJson,
            double mad,
            double mape,
            double rmse,
            double trackingSignal,
            Instant generatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.materialId = materialId;
        this.destinationNodeId = destinationNodeId;
        this.method = method;
        this.forecastedQuantitiesJson = forecastedQuantitiesJson;
        this.historicalBaselineJson = historicalBaselineJson;
        this.periodOverridesJson = periodOverridesJson;
        this.mad = mad;
        this.mape = mape;
        this.rmse = rmse;
        this.trackingSignal = trackingSignal;
        this.generatedAt = generatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getForecastedQuantitiesJson() { return forecastedQuantitiesJson; }
    public void setForecastedQuantitiesJson(String forecastedQuantitiesJson) { this.forecastedQuantitiesJson = forecastedQuantitiesJson; }
    public String getHistoricalBaselineJson() { return historicalBaselineJson; }
    public void setHistoricalBaselineJson(String historicalBaselineJson) { this.historicalBaselineJson = historicalBaselineJson; }
    public String getPeriodOverridesJson() { return periodOverridesJson; }
    public void setPeriodOverridesJson(String periodOverridesJson) { this.periodOverridesJson = periodOverridesJson; }
    public double getMad() { return mad; }
    public void setMad(double mad) { this.mad = mad; }
    public double getMape() { return mape; }
    public void setMape(double mape) { this.mape = mape; }
    public double getRmse() { return rmse; }
    public void setRmse(double rmse) { this.rmse = rmse; }
    public double getTrackingSignal() { return trackingSignal; }
    public void setTrackingSignal(double trackingSignal) { this.trackingSignal = trackingSignal; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}

