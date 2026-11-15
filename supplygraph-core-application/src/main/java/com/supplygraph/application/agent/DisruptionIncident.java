package com.supplygraph.application.agent;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Disruption event ingested by the platform representing a supply-chain anomaly,
 * bottleneck, failure, or regulatory hold.
 */
public record DisruptionIncident(
        String incidentId,
        WorkspaceId workspaceId,
        String title,
        String description,
        String industryPackCode,
        Set<String> affectedNodeIds,
        Set<String> affectedMaterialIds,
        long estimatedDowntimeHours,
        String severity,
        Instant detectedAt
) {
    public DisruptionIncident {
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(industryPackCode, "industryPackCode must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        affectedNodeIds = affectedNodeIds != null ? Set.copyOf(affectedNodeIds) : Set.of();
        affectedMaterialIds = affectedMaterialIds != null ? Set.copyOf(affectedMaterialIds) : Set.of();
    }
}
