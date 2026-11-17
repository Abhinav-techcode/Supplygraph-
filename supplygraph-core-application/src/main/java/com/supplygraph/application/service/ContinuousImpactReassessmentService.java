package com.supplygraph.application.service;

import com.supplygraph.application.port.out.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.impact.ImpactAssessment;
import com.supplygraph.domain.model.monitoring.RiskAlert;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.reassessment.ImpactReassessment;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service performing partial, targeted impact reassessments across affected subgraphs
 * while explicitly preserving unaffected graph entities, assessments, and recovery actions.
 */
@Service
public class ContinuousImpactReassessmentService {

    private static final Logger log = LoggerFactory.getLogger(ContinuousImpactReassessmentService.class);

    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final ImpactAssessmentRepositoryPort impactAssessmentRepository;
    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final ImpactReassessmentRepositoryPort reassessmentRepository;

    public ContinuousImpactReassessmentService(
            DigitalTwinRepositoryPort digitalTwinRepository,
            ImpactAssessmentRepositoryPort impactAssessmentRepository,
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            ImpactReassessmentRepositoryPort reassessmentRepository
    ) {
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.impactAssessmentRepository = Objects.requireNonNull(impactAssessmentRepository, "ImpactAssessmentRepositoryPort must not be null");
        this.recoveryPlanRepository = Objects.requireNonNull(recoveryPlanRepository, "RecoveryPlanRepositoryPort must not be null");
        this.reassessmentRepository = Objects.requireNonNull(reassessmentRepository, "ImpactReassessmentRepositoryPort must not be null");
    }

    @Transactional
    public ImpactReassessment reassessOnEvent(OperationalEvent event, RiskAlert alert) {
        WorkspaceId workspaceId = event.getWorkspaceId();
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElse(null);

        StateVersion previousVersion = state != null ? state.getCurrentVersion() : StateVersion.initial();
        StateVersion newVersion = StateVersion.of(previousVersion.value() + 1L);

        // Advance state version
        if (state != null) {
            newVersion = state.advanceVersion("Continuous impact reassessment triggered by event " + event.getEventId().value());
            digitalTwinRepository.save(state);
        }

        DigitalTwinSnapshot snapshot = state != null ? state.createSnapshot() : null;

        // 1. Identify Target Affected Entity
        String affectedEntity = event.getAffectedEntityId();
        Set<NodeId> affectedNodes = new LinkedHashSet<>();
        Set<EdgeId> affectedLanes = new LinkedHashSet<>();
        Set<MaterialId> affectedMaterials = new LinkedHashSet<>();

        if ("NODE".equalsIgnoreCase(event.getAffectedEntityType())) {
            NodeId rootNode = NodeId.of(affectedEntity);
            affectedNodes.add(rootNode);

            // Traverse downstream graph if available
            if (snapshot != null && snapshot.graph() != null) {
                Queue<NodeId> queue = new ArrayDeque<>();
                queue.add(rootNode);
                while (!queue.isEmpty()) {
                    NodeId curr = queue.poll();
                    for (TransportationLane lane : snapshot.graph().getOutgoingLanes(curr)) {
                        affectedLanes.add(lane.getId());
                        if (affectedNodes.add(lane.getTargetNodeId())) {
                            queue.add(lane.getTargetNodeId());
                        }
                    }
                }
            }
        } else if ("EDGE".equalsIgnoreCase(event.getAffectedEntityType()) || "LANE".equalsIgnoreCase(event.getAffectedEntityType())) {
            EdgeId laneId = EdgeId.of(affectedEntity);
            affectedLanes.add(laneId);
            if (snapshot != null && snapshot.graph() != null) {
                snapshot.graph().findLane(laneId).ifPresent(l -> affectedNodes.add(l.getTargetNodeId()));
            }
        } else if ("MATERIAL".equalsIgnoreCase(event.getAffectedEntityType())) {
            affectedMaterials.add(MaterialId.of(affectedEntity));
        }

        // 2. Identify Preserved vs Invalidated Scopes
        List<String> preservedScope = new ArrayList<>();
        List<String> invalidatedScope = new ArrayList<>();

        if (snapshot != null && snapshot.graph() != null && snapshot.graph().getNodeCount() > 0) {
            // Unaffected nodes are preserved
            for (NodeId nid : affectedNodes) {
                invalidatedScope.add("Node [" + nid.value() + "] affected by event " + event.getEventId().value());
            }
            preservedScope.add("Preserved unaffected supply chain nodes and upstream lanes");
        } else {
            invalidatedScope.add("Entity [" + affectedEntity + "] affected");
            preservedScope.add("Preserved unaffected enterprise context");
        }

        // 3. Find Affected Existing Assessments & Plans
        List<String> affectedAssessments = new ArrayList<>();
        List<ImpactAssessment> assessments = impactAssessmentRepository.findAllByWorkspaceId(workspaceId);
        for (ImpactAssessment a : assessments) {
            boolean hitsNode = a.nodeImpacts().stream().anyMatch(n -> affectedNodes.contains(n.nodeId()));
            boolean hitsMat = a.materialShortages().stream().anyMatch(m -> affectedMaterials.contains(m.materialId()));
            if (hitsNode || hitsMat) {
                affectedAssessments.add(a.id().value());
            }
        }

        List<String> affectedPlans = new ArrayList<>();
        List<RecoveryPlan> plans = recoveryPlanRepository.findAllByWorkspaceId(workspaceId);
        for (RecoveryPlan plan : plans) {
            if (plan.status() == RecoveryPlanStatus.APPROVED || plan.status() == RecoveryPlanStatus.PENDING_APPROVAL) {
                boolean actionHits = plan.actions().stream().anyMatch(action ->
                        (action.targetNodeId() != null && affectedNodes.contains(action.targetNodeId()))
                                || (action.targetMaterialId() != null && affectedMaterials.contains(action.targetMaterialId()))
                );
                if (actionHits) {
                    affectedPlans.add(plan.id().value());
                }
            }
        }

        ImpactReassessment reassessment = ImpactReassessment.create(
                workspaceId,
                event.getEventId(),
                previousVersion,
                newVersion,
                affectedNodes,
                affectedLanes,
                affectedMaterials,
                preservedScope,
                invalidatedScope,
                affectedAssessments,
                affectedPlans
        );

        reassessmentRepository.save(reassessment);
        log.info("Continuous impact reassessment {} completed: {} affected nodes, {} affected plans",
                reassessment.getId().value(), affectedNodes.size(), affectedPlans.size());

        return reassessment;
    }

    public List<ImpactReassessment> getReassessments(WorkspaceId workspaceId) {
        return reassessmentRepository.findByWorkspaceId(workspaceId);
    }
}
