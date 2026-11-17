package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.impact.ImpactAssessment;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving impact assessments.
 */
public interface ImpactAssessmentRepositoryPort {
    void save(ImpactAssessment assessment);
    Optional<ImpactAssessment> findById(ImpactAssessmentId id);
    Optional<ImpactAssessment> findLatestByDisruptionId(DisruptionId disruptionId);
    List<ImpactAssessment> findAllByWorkspaceId(WorkspaceId workspaceId);
    long countByWorkspaceId(WorkspaceId workspaceId);
}

