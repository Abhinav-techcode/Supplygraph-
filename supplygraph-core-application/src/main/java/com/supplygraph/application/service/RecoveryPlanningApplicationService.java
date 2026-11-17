package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.ImpactAssessmentRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.concurguard.api.ConcurGuardEngine;
import com.supplygraph.concurguard.api.DefaultConcurGuardEngine;
import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.model.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.impact.ImpactAssessment;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Application service managing recovery plan formulation, multi-alternative generation,
 * ConcurGuard conflict validation, stale snapshot verification, and explicit human approval.
 */
@Service
public class RecoveryPlanningApplicationService {

    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final DisruptionRepositoryPort disruptionRepository;
    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final ImpactAssessmentRepositoryPort impactAssessmentRepository;
    private final ConcurGuardEngine concurGuardEngine;

    public RecoveryPlanningApplicationService(
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            DisruptionRepositoryPort disruptionRepository,
            DigitalTwinRepositoryPort digitalTwinRepository,
            ImpactAssessmentRepositoryPort impactAssessmentRepository
    ) {
        this.recoveryPlanRepository = Objects.requireNonNull(recoveryPlanRepository, "RecoveryPlanRepositoryPort must not be null");
        this.disruptionRepository = Objects.requireNonNull(disruptionRepository, "DisruptionRepositoryPort must not be null");
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.impactAssessmentRepository = Objects.requireNonNull(impactAssessmentRepository, "ImpactAssessmentRepositoryPort must not be null");
        this.concurGuardEngine = new DefaultConcurGuardEngine();
    }

    @Transactional
    public RecoveryPlan createPlan(
            DisruptionId disruptionId,
            WorkspaceId workspaceId,
            ImpactAssessmentId impactAssessmentId,
            String planName,
            String description,
            List<PlanAction> actions,
            PlanComparisonMetrics metrics
    ) {
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + workspaceId.value()));

        RecoveryPlan plan = RecoveryPlan.createDraft(
                disruptionId,
                workspaceId,
                state.getCurrentVersion(),
                impactAssessmentId,
                planName,
                description,
                actions,
                metrics
        );

        recoveryPlanRepository.save(plan);
        return plan;
    }

    @Transactional
    public List<RecoveryPlan> generateAlternativePlans(DisruptionId disruptionId, WorkspaceId workspaceId) {
        Disruption disruption = disruptionRepository.findById(disruptionId)
                .orElseThrow(() -> new NoSuchElementException("Disruption not found: " + disruptionId.value()));
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + workspaceId.value()));

        ImpactAssessment assessment = impactAssessmentRepository.findLatestByDisruptionId(disruptionId)
                .orElse(null);
        ImpactAssessmentId assessmentId = assessment != null ? assessment.id() : ImpactAssessmentId.generate();

        NodeId targetNode = disruption.affectedNodeIds().stream().findFirst().orElse(NodeId.of("HUB-CENTRAL"));
        MaterialId targetMat = disruption.affectedMaterialIds().stream().findFirst().orElse(MaterialId.of("MATERIAL-A"));

        // Alternative 1: Plan Alpha (Fast Expedited Buffer)
        PlanAction a1 = new PlanAction(
                "ACT-ALPHA-1",
                PlanActionType.EMERGENCY_BUFFER_RELEASE,
                "Release strategic reserve inventory at central distribution hub",
                targetNode,
                targetMat,
                BigDecimal.valueOf(2500),
                "KG",
                BigDecimal.valueOf(45000),
                18L,
                Map.of("bufferLocation", "HUB-CENTRAL", "releaseAuthorization", "CRITICAL_SLA")
        );
        PlanComparisonMetrics m1 = new PlanComparisonMetrics(
                BigDecimal.valueOf(45000), 1.5, 99.2, BigDecimal.ZERO, BigDecimal.valueOf(2500), 65.0, 0.15, 0.92
        );
        RecoveryPlan planAlpha = RecoveryPlan.createDraft(
                disruptionId, workspaceId, state.getCurrentVersion(), assessmentId,
                "Plan Alpha: Rapid Buffer Dispatch",
                "Immediate mitigation utilizing on-hand safety buffer stocks to avert line stoppage.",
                List.of(a1), m1
        );

        // Alternative 2: Plan Beta (Cost-Optimized Dual Sourcing)
        PlanAction b1 = new PlanAction(
                "ACT-BETA-1",
                PlanActionType.ALTERNATIVE_SUPPLIER,
                "Activate secondary qualified supplier contract",
                NodeId.of("SUPPLIER-BACKUP-1"),
                targetMat,
                BigDecimal.valueOf(5000),
                "KG",
                BigDecimal.valueOf(78000),
                48L,
                Map.of("supplierId", "SUPPLIER-BACKUP-1", "pricingTier", "VOLUME_DISCOUNT")
        );
        PlanComparisonMetrics m2 = new PlanComparisonMetrics(
                BigDecimal.valueOf(78000), 3.0, 96.5, BigDecimal.valueOf(200), BigDecimal.valueOf(5000), 80.0, 0.25, 0.88
        );
        RecoveryPlan planBeta = RecoveryPlan.createDraft(
                disruptionId, workspaceId, state.getCurrentVersion(), assessmentId,
                "Plan Beta: Secondary Sourcing Ramp",
                "Medium-term resilience reallocating procurement contracts to qualified secondary suppliers.",
                List.of(b1), m2
        );

        // Alternative 3: Plan Gamma (Production Rebalancing & Overtime)
        PlanAction c1 = new PlanAction(
                "ACT-GAMMA-1",
                PlanActionType.PRODUCTION_RESCHEDULING,
                "Rebalance production line sequence and authorize weekend overtime shift",
                targetNode,
                targetMat,
                BigDecimal.valueOf(3500),
                "KG",
                BigDecimal.valueOf(32000),
                72L,
                Map.of("shiftPattern", "24x7_WEEKEND", "overtimeMultiplier", "1.5")
        );
        PlanComparisonMetrics m3 = new PlanComparisonMetrics(
                BigDecimal.valueOf(32000), 4.5, 92.0, BigDecimal.valueOf(500), BigDecimal.valueOf(3500), 95.0, 0.35, 0.78
        );
        RecoveryPlan planGamma = RecoveryPlan.createDraft(
                disruptionId, workspaceId, state.getCurrentVersion(), assessmentId,
                "Plan Gamma: Internal Capacity Shift",
                "Internal operational adjustment minimizing external procurement costs through overtime scheduling.",
                List.of(c1), m3
        );

        List<RecoveryPlan> plans = List.of(planAlpha, planBeta, planGamma);
        for (RecoveryPlan p : plans) {
            recoveryPlanRepository.save(p);
        }
        return plans;
    }

    @Transactional
    public RecoveryPlan validatePlanWithConcurGuard(RecoveryPlanId planId) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + plan.workspaceId().value()));

        DigitalTwinSnapshot snapshot = state.createSnapshot();

        // Convert plan actions into ActionIntents for ConcurGuard
        List<ActionIntent> intents = new ArrayList<>();
        Instant now = Instant.now();

        for (PlanAction action : plan.actions()) {
            ActionType actionType = switch (action.actionType()) {
                case ALTERNATIVE_SUPPLIER, PURCHASE_ORDER_EXPEDITE -> ActionType.EXPEDITE_PURCHASE_ORDER;
                case INVENTORY_REALLOCATION -> ActionType.REALLOCATE_INVENTORY;
                case PRODUCTION_RESCHEDULING -> ActionType.SPLIT_PRODUCTION_BATCH;
                case EXPEDITED_FREIGHT -> ActionType.REROUTE_SHIPMENT;
                case CAPACITY_REASSIGNMENT -> ActionType.RESERVE_CAPACITY;
                case EMERGENCY_BUFFER_RELEASE -> ActionType.EMERGENCY_BUFFER_DISPATCH;
            };

            CapacityWindow window = new CapacityWindow(now, now.plusSeconds(action.estimatedDurationHours() * 3600L));
            Quantity qty = new Quantity(action.quantity(), UnitOfMeasure.PIECE);

            ActionIntent intent = new ActionIntent(
                    IntentId.of("INTENT-" + action.actionId()),
                    plan.disruptionId().value(),
                    AgentId.of("RECOVERY_PLANNING_AGENT"),
                    actionType,
                    action.targetNodeId() != null ? action.targetNodeId().value() : "TARGET-FACILITY",
                    List.of(),
                    qty,
                    null,
                    window,
                    action.targetNodeId() != null ? action.targetNodeId().value() : "LOCATION",
                    Set.of(),
                    List.of(),
                    snapshot.version(),
                    List.of("PlanAction: " + action.actionId()),
                    List.of(action.description()),
                    action.costEstimate(),
                    0.1,
                    true,
                    false
            );
            intents.add(intent);
        }

        List<Conflict> conflicts = concurGuardEngine.getConflictDetector().detectAllConflicts(intents, snapshot);

        boolean isValid = conflicts.isEmpty();
        List<String> conflictIds = conflicts.stream()
                .map(c -> c.id().value() + " :: " + c.explanation())
                .toList();

        RecoveryPlan validated = plan.markValidated(isValid, conflictIds, now);
        if (isValid) {
            validated = validated.markPendingApproval(now);
        }

        recoveryPlanRepository.save(validated);
        return validated;
    }

    @Transactional
    public RecoveryPlan approvePlan(RecoveryPlanId planId, String approver, String rationale) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + plan.workspaceId().value()));

        RecoveryPlan approved = plan.approve(approver, rationale, state.getCurrentVersion(), Instant.now());
        recoveryPlanRepository.save(approved);
        return approved;
    }

    @Transactional
    public RecoveryPlan rejectPlan(RecoveryPlanId planId, String rejecter, String rationale) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + plan.workspaceId().value()));

        RecoveryPlan rejected = plan.reject(rejecter, rationale, state.getCurrentVersion(), Instant.now());
        recoveryPlanRepository.save(rejected);
        return rejected;
    }

    @Transactional(readOnly = true)
    public RecoveryPlan getPlan(RecoveryPlanId id) {
        return recoveryPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recovery plan not found: " + id.value()));
    }

    @Transactional(readOnly = true)
    public List<RecoveryPlan> listPlansByDisruption(DisruptionId disruptionId) {
        return recoveryPlanRepository.findByDisruptionId(disruptionId);
    }

    @Transactional(readOnly = true)
    public List<RecoveryPlan> listAllPlans(WorkspaceId workspaceId) {
        return recoveryPlanRepository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<RecoveryPlan> listPlans(WorkspaceId workspaceId) {
        return listAllPlans(workspaceId);
    }

    @Transactional(readOnly = true)
    public long countPendingApprovals(WorkspaceId workspaceId) {
        return recoveryPlanRepository.countPendingApprovalByWorkspaceId(workspaceId);
    }

    @Transactional
    public List<RecoveryPlan> generateDefaultAlternatives(DisruptionId disruptionId, WorkspaceId workspaceId) {
        return generateAlternativePlans(disruptionId, workspaceId);
    }
}
