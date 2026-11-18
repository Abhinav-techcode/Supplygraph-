package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.resilience.advanced.AdvancedResilienceMetrics;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for advanced resilience analytics.
 */
public interface AdvancedResilienceRepositoryPort {

    void save(AdvancedResilienceMetrics metrics);

    Optional<AdvancedResilienceMetrics> findLatestByWorkspaceId(String workspaceId);

    List<AdvancedResilienceMetrics> findAll();
}

