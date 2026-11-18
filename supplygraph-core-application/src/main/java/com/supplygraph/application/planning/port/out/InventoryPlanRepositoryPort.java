package com.supplygraph.application.planning.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.InventoryPlan;
import com.supplygraph.domain.model.planning.InventoryPlanId;

import java.util.List;
import java.util.Optional;

public interface InventoryPlanRepositoryPort {
    void save(InventoryPlan plan);
    Optional<InventoryPlan> findById(InventoryPlanId id);
    List<InventoryPlan> findByWorkspaceId(WorkspaceId workspaceId);
}

