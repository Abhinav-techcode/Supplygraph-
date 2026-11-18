package com.supplygraph.application.sustainability.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenario;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenarioId;

import java.util.List;
import java.util.Optional;

public interface EnvironmentalScenarioRepositoryPort {
    void save(EnvironmentalScenario scenario);
    Optional<EnvironmentalScenario> findById(EnvironmentalScenarioId id);
    List<EnvironmentalScenario> findByWorkspaceId(WorkspaceId workspaceId);
}

