package com.supplygraph.application.planning.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.DemandForecastId;

import java.util.List;
import java.util.Optional;

public interface DemandForecastRepositoryPort {
    void save(DemandForecast forecast);
    Optional<DemandForecast> findById(DemandForecastId id);
    List<DemandForecast> findByWorkspaceId(WorkspaceId workspaceId);
}

