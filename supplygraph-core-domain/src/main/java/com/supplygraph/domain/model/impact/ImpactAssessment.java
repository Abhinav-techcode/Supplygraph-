package com.supplygraph.domain.model.impact;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.twin.StateVersion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable assessment of the cascading upstream and downstream consequences of a disruption
 * evaluated deterministically against a specific Digital Twin snapshot version.
 */
public record ImpactAssessment(
        ImpactAssessmentId id,
        DisruptionId disruptionId,
        WorkspaceId workspaceId,
        StateVersion digitalTwinVersion,
        Instant assessmentTimestamp,
        ImpactSeverity overallSeverity,
        List<NodeImpact> nodeImpacts,
        List<MaterialShortageImpact> materialShortages,
        List<CapacityImpact> capacityImpacts,
        List<LogisticsImpact> logisticsImpacts,
        BigDecimal totalEstimatedCost,
        List<String> riskIndicators,
        List<String> assumptions,
        List<String> unknownOrMissingData,
        String status,
        long version
) {
    public ImpactAssessment {
        Objects.requireNonNull(id, "ImpactAssessmentId must not be null");
        Objects.requireNonNull(disruptionId, "DisruptionId must not be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId must not be null");
        Objects.requireNonNull(digitalTwinVersion, "DigitalTwinVersion must not be null");
        Objects.requireNonNull(assessmentTimestamp, "AssessmentTimestamp must not be null");
        Objects.requireNonNull(overallSeverity, "OverallSeverity must not be null");

        nodeImpacts = nodeImpacts == null ? List.of() : Collections.unmodifiableList(List.copyOf(nodeImpacts));
        materialShortages = materialShortages == null ? List.of() : Collections.unmodifiableList(List.copyOf(materialShortages));
        capacityImpacts = capacityImpacts == null ? List.of() : Collections.unmodifiableList(List.copyOf(capacityImpacts));
        logisticsImpacts = logisticsImpacts == null ? List.of() : Collections.unmodifiableList(List.copyOf(logisticsImpacts));
        riskIndicators = riskIndicators == null ? List.of() : Collections.unmodifiableList(List.copyOf(riskIndicators));
        assumptions = assumptions == null ? List.of() : Collections.unmodifiableList(List.copyOf(assumptions));
        unknownOrMissingData = unknownOrMissingData == null ? List.of() : Collections.unmodifiableList(List.copyOf(unknownOrMissingData));
        totalEstimatedCost = totalEstimatedCost == null ? BigDecimal.ZERO : totalEstimatedCost;
        status = status == null ? "COMPLETED" : status;

        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
    }

    public static ImpactAssessment create(
            DisruptionId disruptionId,
            WorkspaceId workspaceId,
            StateVersion digitalTwinVersion,
            ImpactSeverity overallSeverity,
            List<NodeImpact> nodeImpacts,
            List<MaterialShortageImpact> materialShortages,
            List<CapacityImpact> capacityImpacts,
            List<LogisticsImpact> logisticsImpacts,
            BigDecimal totalEstimatedCost,
            List<String> riskIndicators,
            List<String> assumptions,
            List<String> unknownOrMissingData
    ) {
        return new ImpactAssessment(
                ImpactAssessmentId.generate(),
                disruptionId,
                workspaceId,
                digitalTwinVersion,
                Instant.now(),
                overallSeverity,
                nodeImpacts,
                materialShortages,
                capacityImpacts,
                logisticsImpacts,
                totalEstimatedCost,
                riskIndicators,
                assumptions,
                unknownOrMissingData,
                "COMPLETED",
                0L
        );
    }
}

