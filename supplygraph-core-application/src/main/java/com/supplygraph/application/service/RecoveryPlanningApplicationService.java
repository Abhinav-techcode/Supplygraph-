package com.supplygraph.application.service;

import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.domain.error.BusinessRuleException;
import com.supplygraph.domain.error.ResourceNotFoundException;
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
    private com.supplygraph.application.agent.AgentLearningService agentLearning;

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

    /** Optional: when present, human decisions on plans are recorded as outcomes that calibrate the agents' confidence. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAgentLearning(com.supplygraph.application.agent.AgentLearningService agentLearning) {
        this.agentLearning = agentLearning;
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
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + workspaceId.value()));

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
                .orElseThrow(() -> new ResourceNotFoundException("Disruption not found: " + disruptionId.value()));
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + workspaceId.value()));

        ImpactAssessment assessment = impactAssessmentRepository.findLatestByDisruptionId(disruptionId)
                .orElse(null);
        ImpactAssessmentId assessmentId = assessment != null ? assessment.id() : ImpactAssessmentId.generate();

        List<RecoveryOptionDeriver.Option> options = RecoveryOptionDeriver.derive(disruption, state);
        if (options.isEmpty()) {
            throw new BusinessRuleException("No recovery option can be derived from the digital twin for this disruption: it needs affected facilities in the network, "
                    + "a downstream facility with a known requirement, and either buffer stock or an alternative supplier reachable over undisrupted lanes.");
        }
        String[] labels = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon"};
        List<RecoveryPlan> plans = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            RecoveryOptionDeriver.Option o = options.get(i);
            plans.add(RecoveryPlan.createDraft(disruptionId, workspaceId, state.getCurrentVersion(), assessmentId,
                    "Plan " + labels[Math.min(i, labels.length - 1)] + ": " + o.name(), o.description(), List.of(o.action()), o.metrics()));
        }
        for (RecoveryPlan p : plans) {
            recoveryPlanRepository.save(p);
        }
        return plans;
    }

    @Transactional
    public RecoveryPlan validatePlanWithConcurGuard(RecoveryPlanId planId) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + plan.workspaceId().value()));

        Instant now = Instant.now();
        List<Conflict> conflicts = detectConflicts(plan, state.createSnapshot());

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

    /**
     * Read-only ConcurGuard re-evaluation of a plan against the CURRENT twin state. Does not change the plan.
     * Used before converting an approved plan into an execution workflow.
     */
    @Transactional(readOnly = true)
    public List<String> revalidateConflicts(RecoveryPlanId planId) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + plan.workspaceId().value()));
        return detectConflicts(plan, state.createSnapshot()).stream().map(c -> c.id().value() + " :: " + c.explanation()).toList();
    }

    private List<Conflict> detectConflicts(RecoveryPlan plan, DigitalTwinSnapshot snapshot) {
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

            List<ResourceClaim> claims = new ArrayList<>();
            if (action.actionType() == PlanActionType.EMERGENCY_BUFFER_RELEASE || action.actionType() == PlanActionType.INVENTORY_REALLOCATION) {
                String sourceFacility = (action.parameters() != null && action.parameters().containsKey("sourceNode"))
                        ? action.parameters().get("sourceNode")
                        : (action.targetNodeId() != null ? action.targetNodeId().value() : null);
                if (sourceFacility != null && action.targetMaterialId() != null) {
                    ResourceIdentity srcRes = ResourceIdentity.of(
                            "INVENTORY_ACCOUNT",
                            action.targetMaterialId().value(),
                            sourceFacility
                    );
                    claims.add(ResourceClaim.consumptive(srcRes, qty, window));
                }
            } else if (action.actionType() == PlanActionType.PRODUCTION_RESCHEDULING) {
                if (action.targetMaterialId() != null && action.targetNodeId() != null) {
                    ResourceIdentity resIdentity = ResourceIdentity.of(
                            "INVENTORY_ACCOUNT",
                            action.targetMaterialId().value(),
                            action.targetNodeId().value()
                    );
                    claims.add(ResourceClaim.consumptive(resIdentity, qty, window));
                }
            }

            ActionIntent intent = new ActionIntent(
                    IntentId.of("INTENT-" + action.actionId()),
                    plan.disruptionId().value(),
                    AgentId.of("RECOVERY_PLANNING_AGENT"),
                    actionType,
                    action.targetNodeId() != null ? action.targetNodeId().value() : "TARGET-FACILITY",
                    claims,
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

        return concurGuardEngine.getConflictDetector().detectAllConflicts(intents, snapshot);
    }

    @Transactional
    public RecoveryPlan approvePlan(RecoveryPlanId planId, String approver, String rationale) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + plan.workspaceId().value()));

        if (!plan.sourceTwinVersion().equals(state.getCurrentVersion())) {
            ConflictException stale = new ConflictException("The network changed after this plan was made (plan was made on twin v" + plan.sourceTwinVersion().value()
                    + ", now v" + state.getCurrentVersion().value() + "), so the options must be derived again before approval.");
            stale.with("planVersion", plan.sourceTwinVersion().value()).with("currentVersion", state.getCurrentVersion().value());
            throw stale;
        }
        RecoveryPlan approved = plan.approve(approver, rationale, state.getCurrentVersion(), Instant.now());
        recoveryPlanRepository.save(approved);
        if (agentLearning != null) {
            agentLearning.recordIncidentOutcome(plan.workspaceId(), plan.disruptionId().value(), true, "Plan approved by " + approver);
        }
        return approved;
    }

    @Transactional
    public RecoveryPlan rejectPlan(RecoveryPlanId planId, String rejecter, String rationale) {
        RecoveryPlan plan = getPlan(planId);
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(plan.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + plan.workspaceId().value()));

        RecoveryPlan rejected = plan.reject(rejecter, rationale, state.getCurrentVersion(), Instant.now());
        recoveryPlanRepository.save(rejected);
        // Rejecting one alternative is a normal choice; the agents are only marked wrong when every plan for the incident was turned down.
        if (agentLearning != null && recoveryPlanRepository.findByDisruptionId(plan.disruptionId()).stream()
                .allMatch(p -> p.status() == RecoveryPlanStatus.REJECTED || p.id().equals(rejected.id()))) {
            agentLearning.recordIncidentOutcome(plan.workspaceId(), plan.disruptionId().value(), false, "All plans rejected; last by " + rejecter);
        }
        return rejected;
    }

    @Transactional(readOnly = true)
    public RecoveryPlan getPlan(RecoveryPlanId id) {
        return recoveryPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery plan not found: " + id.value()));
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
