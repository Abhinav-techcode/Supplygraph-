package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Finite capacity production schedule resolving machine contention and minimizing changeovers.
 */
public record FiniteProductionSchedule(
        ProductionScheduleId id,
        WorkspaceId workspaceId,
        NodeId plantId,
        String scheduleName,
        List<ScheduledProductionJob> scheduledJobs,
        double averageCapacityUtilization,
        Instant generatedAt
) implements Serializable {
    public FiniteProductionSchedule {
        Objects.requireNonNull(id, "Production schedule ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(plantId, "Plant ID cannot be null");
        Objects.requireNonNull(scheduleName, "Schedule name cannot be null");
        scheduledJobs = List.copyOf(scheduledJobs);
        Objects.requireNonNull(generatedAt, "Generation timestamp cannot be null");
    }

    public int getJobCount() {
        return scheduledJobs.size();
    }
}

