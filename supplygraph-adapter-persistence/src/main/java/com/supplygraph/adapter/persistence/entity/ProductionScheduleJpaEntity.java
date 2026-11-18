package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_production_schedules")
public class ProductionScheduleJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "plant_id", length = 64, nullable = false)
    private String plantId;

    @Column(name = "schedule_name", length = 255, nullable = false)
    private String scheduleName;

    @Column(name = "scheduled_jobs_json", columnDefinition = "TEXT", nullable = false)
    private String scheduledJobsJson;

    @Column(name = "average_capacity_utilization", nullable = false)
    private double averageCapacityUtilization;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public ProductionScheduleJpaEntity() {}

    public ProductionScheduleJpaEntity(
            String id,
            String workspaceId,
            String plantId,
            String scheduleName,
            String scheduledJobsJson,
            double averageCapacityUtilization,
            Instant generatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.plantId = plantId;
        this.scheduleName = scheduleName;
        this.scheduledJobsJson = scheduledJobsJson;
        this.averageCapacityUtilization = averageCapacityUtilization;
        this.generatedAt = generatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getScheduleName() { return scheduleName; }
    public void setScheduleName(String scheduleName) { this.scheduleName = scheduleName; }
    public String getScheduledJobsJson() { return scheduledJobsJson; }
    public void setScheduledJobsJson(String scheduledJobsJson) { this.scheduledJobsJson = scheduledJobsJson; }
    public double getAverageCapacityUtilization() { return averageCapacityUtilization; }
    public void setAverageCapacityUtilization(double averageCapacityUtilization) { this.averageCapacityUtilization = averageCapacityUtilization; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}

