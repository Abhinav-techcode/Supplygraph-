package com.supplygraph.application.planning.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.planning.PlanningScenarioId;

import java.util.List;
import java.util.Optional;

public interface PlanningScenarioRepositoryPort {
    void save(PlanningScenarioComparison comparison);
    Optional<PlanningScenarioComparison> findById(PlanningScenarioId id);
    List<PlanningScenarioComparison> findByWorkspaceId(WorkspaceId workspaceId);
}

