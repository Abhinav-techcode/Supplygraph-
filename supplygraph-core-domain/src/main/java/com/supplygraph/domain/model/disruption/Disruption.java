package com.supplygraph.domain.model.disruption;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable domain aggregate representing a supply chain disruption incident.
 * Enforces valid lifecycle state transitions, boundary checks, and version tracking.
 */
public record Disruption(
        DisruptionId id,
        WorkspaceId workspaceId,
        String industryCode,
        String scenarioType,
        String title,
        String description,
        DisruptionCategory category,
        DisruptionSeverity severity,
        DisruptionStatus status,
        Set<NodeId> affectedNodeIds,
        Set<EdgeId> affectedLaneIds,
        Set<MaterialId> affectedMaterialIds,
        Instant startTime,
        Instant expectedEndTime,
        DisruptionImpactParameters impactParameters,
        String sourceEvidence,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public Disruption {
        Objects.requireNonNull(id, "DisruptionId must not be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId must not be null");
        Objects.requireNonNull(title, "Title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Objects.requireNonNull(category, "Category must not be null");
        Objects.requireNonNull(severity, "Severity must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(startTime, "StartTime must not be null");
        Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        Objects.requireNonNull(updatedAt, "UpdatedAt must not be null");

        if (expectedEndTime != null && expectedEndTime.isBefore(startTime)) {
            throw new IllegalArgumentException("ExpectedEndTime cannot be before startTime: " + expectedEndTime + " < " + startTime);
        }

        affectedNodeIds = affectedNodeIds == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(affectedNodeIds));
        affectedLaneIds = affectedLaneIds == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(affectedLaneIds));
        affectedMaterialIds = affectedMaterialIds == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(affectedMaterialIds));
        impactParameters = impactParameters == null ? DisruptionImpactParameters.empty() : impactParameters;
        industryCode = industryCode == null ? "GENERIC" : industryCode;
        scenarioType = scenarioType == null ? "DEFAULT" : scenarioType;
        description = description == null ? "" : description;
        sourceEvidence = sourceEvidence == null ? "" : sourceEvidence;

        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
    }

    public static Disruption createDraft(
            WorkspaceId workspaceId,
            String industryCode,
            String scenarioType,
            String title,
            String description,
            DisruptionCategory category,
            DisruptionSeverity severity,
            Set<NodeId> affectedNodeIds,
            Set<EdgeId> affectedLaneIds,
            Set<MaterialId> affectedMaterialIds,
            Instant startTime,
            Instant expectedEndTime,
            DisruptionImpactParameters impactParameters,
            String sourceEvidence
    ) {
        Instant now = Instant.now();
        return new Disruption(
                DisruptionId.generate(),
                workspaceId,
                industryCode,
                scenarioType,
                title,
                description,
                category,
                severity,
                DisruptionStatus.DRAFT,
                affectedNodeIds,
                affectedLaneIds,
                affectedMaterialIds,
                startTime,
                expectedEndTime,
                impactParameters,
                sourceEvidence,
                now,
                now,
                0L
        );
    }

    public Disruption validate(Instant timestamp) {
        ensureCanTransition(DisruptionStatus.VALIDATED);
        return new Disruption(
                id, workspaceId, industryCode, scenarioType, title, description,
                category, severity, DisruptionStatus.VALIDATED,
                affectedNodeIds, affectedLaneIds, affectedMaterialIds,
                startTime, expectedEndTime, impactParameters, sourceEvidence,
                createdAt, timestamp, version + 1
        );
    }

    public Disruption activate(Instant timestamp) {
        ensureCanTransition(DisruptionStatus.ACTIVE);
        return new Disruption(
                id, workspaceId, industryCode, scenarioType, title, description,
                category, severity, DisruptionStatus.ACTIVE,
                affectedNodeIds, affectedLaneIds, affectedMaterialIds,
                startTime, expectedEndTime, impactParameters, sourceEvidence,
                createdAt, timestamp, version + 1
        );
    }

    public Disruption resolve(Instant timestamp) {
        ensureCanTransition(DisruptionStatus.RESOLVED);
        return new Disruption(
                id, workspaceId, industryCode, scenarioType, title, description,
                category, severity, DisruptionStatus.RESOLVED,
                affectedNodeIds, affectedLaneIds, affectedMaterialIds,
                startTime, timestamp, impactParameters, sourceEvidence,
                createdAt, timestamp, version + 1
        );
    }

    public Disruption cancel(Instant timestamp) {
        ensureCanTransition(DisruptionStatus.CANCELLED);
        return new Disruption(
                id, workspaceId, industryCode, scenarioType, title, description,
                category, severity, DisruptionStatus.CANCELLED,
                affectedNodeIds, affectedLaneIds, affectedMaterialIds,
                startTime, expectedEndTime, impactParameters, sourceEvidence,
                createdAt, timestamp, version + 1
        );
    }

    public Disruption update(
            String newTitle,
            String newDescription,
            DisruptionSeverity newSeverity,
            Set<NodeId> newNodes,
            Set<EdgeId> newLanes,
            Set<MaterialId> newMaterials,
            Instant newExpectedEndTime,
            DisruptionImpactParameters newParams,
            String newEvidence,
            Instant timestamp
    ) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot update disruption in terminal state: " + status);
        }
        return new Disruption(
                id, workspaceId, industryCode, scenarioType,
                newTitle != null ? newTitle : title,
                newDescription != null ? newDescription : description,
                category,
                newSeverity != null ? newSeverity : severity,
                status,
                newNodes != null ? newNodes : affectedNodeIds,
                newLanes != null ? newLanes : affectedLaneIds,
                newMaterials != null ? newMaterials : affectedMaterialIds,
                startTime,
                newExpectedEndTime != null ? newExpectedEndTime : expectedEndTime,
                newParams != null ? newParams : impactParameters,
                newEvidence != null ? newEvidence : sourceEvidence,
                createdAt,
                timestamp,
                version + 1
        );
    }

    private void ensureCanTransition(DisruptionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid disruption lifecycle transition from " + status + " to " + target);
        }
    }
}

