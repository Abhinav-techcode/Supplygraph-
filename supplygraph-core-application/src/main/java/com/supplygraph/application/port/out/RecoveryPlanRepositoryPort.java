package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving recovery plans.
 */
public interface RecoveryPlanRepositoryPort {
    void save(RecoveryPlan plan);
    Optional<RecoveryPlan> findById(RecoveryPlanId id);
    List<RecoveryPlan> findByDisruptionId(DisruptionId disruptionId);
    List<RecoveryPlan> findAllByWorkspaceId(WorkspaceId workspaceId);
    long countPendingApprovalByWorkspaceId(WorkspaceId workspaceId);
}

