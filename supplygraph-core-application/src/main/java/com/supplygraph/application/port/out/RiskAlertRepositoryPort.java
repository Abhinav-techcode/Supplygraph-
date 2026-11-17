package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.monitoring.RiskAlert;
import com.supplygraph.domain.model.monitoring.RiskAlertId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for operational risk alerts.
 */
public interface RiskAlertRepositoryPort {
    void save(RiskAlert alert);
    Optional<RiskAlert> findById(RiskAlertId id);
    List<RiskAlert> findByWorkspaceId(WorkspaceId workspaceId);
    List<RiskAlert> findActiveAlerts(WorkspaceId workspaceId);
    Optional<RiskAlert> findActiveBySuppressionKey(String suppressionKey);
}

