package com.supplygraph.domain.model.reassessment;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.twin.StateVersion;

import java.time.Instant;
import java.util.*;

/**
 * Result of a targeted continuous impact reassessment.
 * Explicitly separates affected subgraphs from preserved unaffected scopes.
 */
public class ImpactReassessment {

    private final ImpactReassessmentId id;
    private final WorkspaceId workspaceId;
    private final EventId triggerEventId;
    private final StateVersion previousStateVersion;
    private final StateVersion newStateVersion;
    private final Set<NodeId> affectedNodeIds;
    private final Set<EdgeId> affectedLaneIds;
    private final Set<MaterialId> affectedMaterialIds;
    private final List<String> preservedScope;
    private final List<String> invalidatedScope;
    private final List<String> affectedAssessmentIds;
    private final List<String> affectedPlanIds;
    private final Instant reassessedAt;

    public ImpactReassessment(
            ImpactReassessmentId id,
            WorkspaceId workspaceId,
            EventId triggerEventId,
            StateVersion previousStateVersion,
            StateVersion newStateVersion,
            Set<NodeId> affectedNodeIds,
            Set<EdgeId> affectedLaneIds,
            Set<MaterialId> affectedMaterialIds,
            List<String> preservedScope,
            List<String> invalidatedScope,
            List<String> affectedAssessmentIds,
            List<String> affectedPlanIds,
            Instant reassessedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.triggerEventId = triggerEventId;
        this.previousStateVersion = previousStateVersion != null ? previousStateVersion : StateVersion.initial();
        this.newStateVersion = newStateVersion != null ? newStateVersion : this.previousStateVersion;
        this.affectedNodeIds = affectedNodeIds != null ? Set.copyOf(affectedNodeIds) : Collections.emptySet();
        this.affectedLaneIds = affectedLaneIds != null ? Set.copyOf(affectedLaneIds) : Collections.emptySet();
        this.affectedMaterialIds = affectedMaterialIds != null ? Set.copyOf(affectedMaterialIds) : Collections.emptySet();
        this.preservedScope = preservedScope != null ? List.copyOf(preservedScope) : Collections.emptyList();
        this.invalidatedScope = invalidatedScope != null ? List.copyOf(invalidatedScope) : Collections.emptyList();
        this.affectedAssessmentIds = affectedAssessmentIds != null ? List.copyOf(affectedAssessmentIds) : Collections.emptyList();
        this.affectedPlanIds = affectedPlanIds != null ? List.copyOf(affectedPlanIds) : Collections.emptyList();
        this.reassessedAt = reassessedAt != null ? reassessedAt : Instant.now();
    }

    public static ImpactReassessment create(
            WorkspaceId workspaceId,
            EventId triggerEventId,
            StateVersion previousVersion,
            StateVersion newVersion,
            Set<NodeId> affectedNodes,
            Set<EdgeId> affectedLanes,
            Set<MaterialId> affectedMaterials,
            List<String> preservedScope,
            List<String> invalidatedScope,
            List<String> affectedAssessments,
            List<String> affectedPlans
    ) {
        return new ImpactReassessment(
                ImpactReassessmentId.generate(),
                workspaceId,
                triggerEventId,
                previousVersion,
                newVersion,
                affectedNodes,
                affectedLanes,
                affectedMaterials,
                preservedScope,
                invalidatedScope,
                affectedAssessments,
                affectedPlans,
                Instant.now()
        );
    }

    public ImpactReassessmentId getId() {
        return id;
    }

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public EventId getTriggerEventId() {
        return triggerEventId;
    }

    public StateVersion getPreviousStateVersion() {
        return previousStateVersion;
    }

    public StateVersion getNewStateVersion() {
        return newStateVersion;
    }

    public Set<NodeId> getAffectedNodeIds() {
        return affectedNodeIds;
    }

    public Set<EdgeId> getAffectedLaneIds() {
        return affectedLaneIds;
    }

    public Set<MaterialId> getAffectedMaterialIds() {
        return affectedMaterialIds;
    }

    public List<String> getPreservedScope() {
        return preservedScope;
    }

    public List<String> getInvalidatedScope() {
        return invalidatedScope;
    }

    public List<String> getAffectedAssessmentIds() {
        return affectedAssessmentIds;
    }

    public List<String> getAffectedPlanIds() {
        return affectedPlanIds;
    }

    public Instant getReassessedAt() {
        return reassessedAt;
    }
}

