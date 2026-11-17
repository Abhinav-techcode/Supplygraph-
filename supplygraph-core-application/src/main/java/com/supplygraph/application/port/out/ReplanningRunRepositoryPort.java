package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningRunId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for adaptive replanning executions.
 */
public interface ReplanningRunRepositoryPort {
    void save(ReplanningRun run);
    Optional<ReplanningRun> findById(ReplanningRunId id);
    List<ReplanningRun> findByWorkspaceId(WorkspaceId workspaceId);
}

