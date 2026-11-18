package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Multi-scenario comparative evaluation metric aggregate supporting executive trade-off decisions.
 */
public record PlanningScenarioComparison(
        PlanningScenarioId scenarioId,
        WorkspaceId workspaceId,
        String scenarioName,
        PlanningScenarioType scenarioType,
        PlanningPlanStatus status,
        BigDecimal totalProjectedCost,
        double customerServiceFillRate,
        double averageLeadTimeDays,
        double carbonFootprintCo2Tons,
        String evaluatedBy,
        Instant evaluatedAt
) implements Serializable {
    public PlanningScenarioComparison {
        Objects.requireNonNull(scenarioId, "Scenario ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(scenarioName, "Scenario name cannot be null");
        Objects.requireNonNull(scenarioType, "Scenario type cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(totalProjectedCost, "Projected cost cannot be null");
        Objects.requireNonNull(evaluatedBy, "Evaluated by cannot be null");
        Objects.requireNonNull(evaluatedAt, "Evaluation timestamp cannot be null");
    }

    public boolean isApproved() {
        return status == PlanningPlanStatus.APPROVED;
    }
}

