package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.intelligence.NetworkResilienceAssessment;
import com.supplygraph.domain.model.intelligence.ResilienceAssessmentId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for network resilience indices, single-points-of-failure audits, and topological risk evaluations.
 */
public interface ResilienceRepositoryPort {

    void save(NetworkResilienceAssessment assessment);

    Optional<NetworkResilienceAssessment> findById(ResilienceAssessmentId id);

    Optional<NetworkResilienceAssessment> findLatestByWorkspaceId(String workspaceId);

    List<NetworkResilienceAssessment> findHistoryByWorkspaceId(String workspaceId);
}

