package com.supplygraph.domain.model.resilience.advanced;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise resilience analytics capturing Time-to-Survive (TTS), Time-to-Recover (TTR),
 * critical supplier exposure, and recovery flexibility.
 */
public record AdvancedResilienceMetrics(
        String assessmentId,
        String workspaceId,
        double timeToSurviveDays,
        double timeToRecoverDays,
        double criticalSupplierExposureIndex,
        double recoveryFlexibilityIndex,
        int bottleneckNodeCount,
        double compositeResilienceScore,
        List<NodeResilienceMetric> nodeResilienceBreakdown,
        Instant calculatedAt
) implements Serializable {

    public AdvancedResilienceMetrics {
        Objects.requireNonNull(assessmentId, "assessmentId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        nodeResilienceBreakdown = nodeResilienceBreakdown == null ? List.of() : List.copyOf(nodeResilienceBreakdown);
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
    }

    public static AdvancedResilienceMetrics of(
            String assessmentId,
            String workspaceId,
            double timeToSurviveDays,
            double timeToRecoverDays,
            double criticalSupplierExposureIndex,
            double recoveryFlexibilityIndex,
            int bottleneckNodeCount,
            double compositeResilienceScore,
            List<NodeResilienceMetric> nodeResilienceBreakdown,
            Instant calculatedAt
    ) {
        return new AdvancedResilienceMetrics(
                assessmentId, workspaceId, timeToSurviveDays, timeToRecoverDays,
                criticalSupplierExposureIndex, recoveryFlexibilityIndex, bottleneckNodeCount,
                compositeResilienceScore, nodeResilienceBreakdown, calculatedAt
        );
    }
}
