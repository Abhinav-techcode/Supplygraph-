package com.supplygraph.application.planning.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.domain.model.planning.ProductionScheduleId;

import java.util.List;
import java.util.Optional;

public interface ProductionScheduleRepositoryPort {
    void save(FiniteProductionSchedule schedule);
    Optional<FiniteProductionSchedule> findById(ProductionScheduleId id);
    List<FiniteProductionSchedule> findByWorkspaceId(WorkspaceId workspaceId);
}

