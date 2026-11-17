package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Application service managing disruption incident lifecycles, entity validation against digital twin snapshots,
 * and optimistic concurrency control.
 */
@Service
public class DisruptionApplicationService {

    private final DisruptionRepositoryPort disruptionRepository;
    private final DigitalTwinRepositoryPort digitalTwinRepository;

    public DisruptionApplicationService(
            DisruptionRepositoryPort disruptionRepository,
            DigitalTwinRepositoryPort digitalTwinRepository
    ) {
        this.disruptionRepository = Objects.requireNonNull(disruptionRepository, "DisruptionRepositoryPort must not be null");
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
    }

    @Transactional
    public Disruption createDisruption(
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
        // Validate entities against Digital Twin
        validateEntitiesAgainstDigitalTwin(workspaceId, affectedNodeIds, affectedLaneIds, affectedMaterialIds);

        Disruption disruption = Disruption.createDraft(
                workspaceId,
                industryCode,
                scenarioType,
                title,
                description,
                category,
                severity,
                affectedNodeIds,
                affectedLaneIds,
                affectedMaterialIds,
                startTime,
                expectedEndTime,
                impactParameters,
                sourceEvidence
        );

        disruptionRepository.save(disruption);
        return disruption;
    }

    @Transactional
    public Disruption validateDisruption(DisruptionId id) {
        Disruption disruption = getDisruption(id);
        // Ensure entities still exist in Digital Twin
        validateEntitiesAgainstDigitalTwin(disruption.workspaceId(), disruption.affectedNodeIds(), disruption.affectedLaneIds(), disruption.affectedMaterialIds());

        Disruption validated = disruption.validate(Instant.now());
        disruptionRepository.save(validated);
        return validated;
    }

    @Transactional
    public Disruption activateDisruption(DisruptionId id) {
        Disruption disruption = getDisruption(id);
        Disruption active = disruption.activate(Instant.now());
        disruptionRepository.save(active);
        return active;
    }

    @Transactional
    public Disruption resolveDisruption(DisruptionId id) {
        Disruption disruption = getDisruption(id);
        Disruption resolved = disruption.resolve(Instant.now());
        disruptionRepository.save(resolved);
        return resolved;
    }

    @Transactional
    public Disruption cancelDisruption(DisruptionId id) {
        Disruption disruption = getDisruption(id);
        Disruption cancelled = disruption.cancel(Instant.now());
        disruptionRepository.save(cancelled);
        return cancelled;
    }

    @Transactional
    public Disruption updateDisruption(
            DisruptionId id,
            long expectedVersion,
            String newTitle,
            String newDescription,
            DisruptionSeverity newSeverity,
            Set<NodeId> newNodes,
            Set<EdgeId> newLanes,
            Set<MaterialId> newMaterials,
            Instant newExpectedEndTime,
            DisruptionImpactParameters newParams,
            String newEvidence
    ) {
        Disruption disruption = getDisruption(id);
        if (disruption.version() != expectedVersion) {
            throw new IllegalStateException("Optimistic concurrency conflict on disruption " + id.value()
                    + ": expected version " + expectedVersion + " but was " + disruption.version());
        }

        if (newNodes != null || newLanes != null || newMaterials != null) {
            validateEntitiesAgainstDigitalTwin(
                    disruption.workspaceId(),
                    newNodes != null ? newNodes : disruption.affectedNodeIds(),
                    newLanes != null ? newLanes : disruption.affectedLaneIds(),
                    newMaterials != null ? newMaterials : disruption.affectedMaterialIds()
            );
        }

        Disruption updated = disruption.update(
                newTitle, newDescription, newSeverity, newNodes, newLanes, newMaterials,
                newExpectedEndTime, newParams, newEvidence, Instant.now()
        );
        disruptionRepository.save(updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public Disruption getDisruption(DisruptionId id) {
        return disruptionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Disruption not found: " + id.value()));
    }

    @Transactional(readOnly = true)
    public List<Disruption> listDisruptions(WorkspaceId workspaceId) {
        return disruptionRepository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public long countActiveDisruptions(WorkspaceId workspaceId) {
        return disruptionRepository.countActiveByWorkspaceId(workspaceId);
    }

    @Transactional
    public Disruption createDraft(
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
        return createDisruption(
                workspaceId, industryCode, scenarioType, title, description,
                category, severity, affectedNodeIds, affectedLaneIds, affectedMaterialIds,
                startTime, expectedEndTime, impactParameters, sourceEvidence
        );
    }

    @Transactional
    public Disruption validateDisruption(DisruptionId id, WorkspaceId wsId, Instant timestamp) {
        return validateDisruption(id);
    }

    @Transactional
    public Disruption activateDisruption(DisruptionId id, WorkspaceId wsId, Instant timestamp) {
        return activateDisruption(id);
    }

    @Transactional
    public Disruption resolveDisruption(DisruptionId id, WorkspaceId wsId, Instant timestamp) {
        return resolveDisruption(id);
    }

    private void validateEntitiesAgainstDigitalTwin(
            WorkspaceId workspaceId,
            Set<NodeId> nodeIds,
            Set<EdgeId> laneIds,
            Set<MaterialId> materialIds
    ) {
        Optional<DigitalTwinState> twinOpt = digitalTwinRepository.findByWorkspaceId(workspaceId);
        if (twinOpt.isEmpty()) {
            return; // If digital twin not initialized yet for workspace, skip
        }

        DigitalTwinSnapshot snapshot = twinOpt.get().createSnapshot();

        if (nodeIds != null && snapshot.graph() != null && snapshot.graph().getNodeCount() > 0) {
            for (NodeId nid : nodeIds) {
                if (!snapshot.graph().containsNode(nid)) {
                    throw new IllegalArgumentException("Unknown node referenced in disruption: " + nid.value());
                }
            }
        }

        if (laneIds != null && snapshot.graph() != null && snapshot.graph().getLaneCount() > 0) {
            for (EdgeId eid : laneIds) {
                if (snapshot.graph().findLane(eid).isEmpty()) {
                    throw new IllegalArgumentException("Unknown transportation lane referenced in disruption: " + eid.value());
                }
            }
        }

        if (materialIds != null && snapshot.bom() != null && snapshot.bom().getMaterialCount() > 0) {
            for (MaterialId mid : materialIds) {
                if (snapshot.bom().findMaterial(mid).isEmpty()) {
                    throw new IllegalArgumentException("Unknown material referenced in disruption: " + mid.value());
                }
            }
        }
    }
}

