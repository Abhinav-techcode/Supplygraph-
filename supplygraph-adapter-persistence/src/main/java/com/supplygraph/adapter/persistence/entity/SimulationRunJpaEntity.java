package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "plan_id", length = 64)
    private String planId;

    @Column(name = "scenario_id", length = 64, nullable = false)
    private String scenarioId;

    @Column(name = "source_twin_version", nullable = false)
    private Long sourceTwinVersion;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "processed_event_count", nullable = false)
    private int processedEventCount;

    @Column(name = "service_level_percentage", nullable = false)
    private double serviceLevelPercentage;

    @Column(name = "total_shortage_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalShortageQuantity;

    @Column(name = "max_delay_seconds", nullable = false)
    private long maxDelaySeconds;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

    @Column(name = "assumptions_json", columnDefinition = "TEXT")
    private String assumptionsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SimulationRunJpaEntity() {
    }

    public SimulationRunJpaEntity(
            String id, String workspaceId, String disruptionId, String planId,
            String scenarioId, Long sourceTwinVersion, Instant startTime, Instant endTime,
            int processedEventCount, double serviceLevelPercentage, BigDecimal totalShortageQuantity,
            long maxDelaySeconds, String status, String warningsJson, String assumptionsJson, Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.disruptionId = Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        this.planId = planId;
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        this.sourceTwinVersion = Objects.requireNonNull(sourceTwinVersion, "sourceTwinVersion must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.processedEventCount = processedEventCount;
        this.serviceLevelPercentage = serviceLevelPercentage;
        this.totalShortageQuantity = totalShortageQuantity == null ? BigDecimal.ZERO : totalShortageQuantity;
        this.maxDelaySeconds = maxDelaySeconds;
        this.status = status == null ? "COMPLETED" : status;
        this.warningsJson = warningsJson;
        this.assumptionsJson = assumptionsJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getDisruptionId() { return disruptionId; }
    public String getPlanId() { return planId; }
    public String getScenarioId() { return scenarioId; }
    public Long getSourceTwinVersion() { return sourceTwinVersion; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public int getProcessedEventCount() { return processedEventCount; }
    public double getServiceLevelPercentage() { return serviceLevelPercentage; }
    public BigDecimal getTotalShortageQuantity() { return totalShortageQuantity; }
    public long getMaxDelaySeconds() { return maxDelaySeconds; }
    public String getStatus() { return status; }
    public String getWarningsJson() { return warningsJson; }
    public String getAssumptionsJson() { return assumptionsJson; }
    public Instant getCreatedAt() { return createdAt; }
}

