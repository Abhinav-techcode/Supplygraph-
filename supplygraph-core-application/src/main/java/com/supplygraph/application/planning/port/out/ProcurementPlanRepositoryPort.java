package com.supplygraph.application.planning.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.domain.model.planning.ProcurementPlanId;

import java.util.List;
import java.util.Optional;

public interface ProcurementPlanRepositoryPort {
    void save(ProcurementPlan plan);
    Optional<ProcurementPlan> findById(ProcurementPlanId id);
    List<ProcurementPlan> findByWorkspaceId(WorkspaceId workspaceId);
}

