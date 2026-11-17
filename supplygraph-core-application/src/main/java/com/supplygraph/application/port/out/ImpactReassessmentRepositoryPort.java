package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.reassessment.ImpactReassessment;
import com.supplygraph.domain.model.reassessment.ImpactReassessmentId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for impact reassessments.
 */
public interface ImpactReassessmentRepositoryPort {
    void save(ImpactReassessment reassessment);
    Optional<ImpactReassessment> findById(ImpactReassessmentId id);
    List<ImpactReassessment> findByWorkspaceId(WorkspaceId workspaceId);
}

