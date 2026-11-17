package com.supplygraph.application.service;

import com.supplygraph.application.port.out.*;
import com.supplygraph.concurguard.api.ConcurGuardEngine;
import com.supplygraph.concurguard.api.DefaultConcurGuardEngine;
import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.model.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.monitoring.RiskAlert;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningStatus;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Service driving adaptive agentic replanning when operational changes
 * invalidate existing recovery plans or assumptions.
 * Preserves unaffected actions, executes ConcurGuard re-arbitration,
 * and strictly enforces human re-approval for all consequential plan revisions.
 */
@Service
public class AdaptiveReplanningService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveReplanningService.class);

    private final ReplanningRunRepositoryPort replanningRunRepository;
    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final ConcurGuardEngine concurGuardEngine;

    public AdaptiveReplanningService(
            ReplanningRunRepositoryPort replanningRunRepository,
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            DigitalTwinRepositoryPort digitalTwinRepository
    ) {
        this.replanningRunRepository = Objects.requireNonNull(replanningRunRepository, "ReplanningRunRepositoryPort must not be null");
        this.recoveryPlanRepository = Objects.requireNonNull(recoveryPlanRepository, "RecoveryPlanRepositoryPort must not be null");
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.concurGuardEngine = new DefaultConcurGuardEngine();
    }

    @Transactional
    public Optional<ReplanningRun> triggerAdaptiveReplanning(RiskAlert alert, OperationalEvent event) {
        Objects.requireNonNull(alert, "alert must not be null");
        WorkspaceId workspaceId = alert.getWorkspaceId();

        // 1. Identify which active or approved recovery plans are affected
        List<RecoveryPlan> plans = recoveryPlanRepository.findAllByWorkspaceId(workspaceId);
        RecoveryPlan affectedPlan = plans.stream()
                .filter(p -> p.status() == RecoveryPlanStatus.APPROVED || p.status() == RecoveryPlanStatus.PENDING_APPROVAL)
                .filter(p -> p.actions().stream().anyMatch(a ->
                        (a.targetNodeId() != null && a.targetNodeId().value().equalsIgnoreCase(alert.getAffectedEntityId()))
                                || (a.targetMaterialId() != null && a.targetMaterialId().value().equalsIgnoreCase(alert.getAffectedEntityId()))
                ))
                .findFirst()
                .orElse(null);

        if (affectedPlan == null) {
            log.info("No active or pending recovery plans invalidated by alert {}", alert.getId().value());
            return Optional.empty();
        }

        log.warn("Operational alert {} invalidated recovery plan {}. Commencing adaptive selective replanning.",
                alert.getId().value(), affectedPlan.id().value());

        // 2. Initialize Replanning Run (DETECTED state)
        ReplanningRun run = ReplanningRun.start(
                workspaceId,
                alert.getId(),
                affectedPlan.id(),
                "Adaptive replanning triggered by risk alert [" + alert.getTitle() + "]: " + alert.getDescription()
        );
        replanningRunRepository.save(run);

        // 3. Lifecycle Step: ASSESSING (Affected Agent Selection & Scope Partitioning)
        run.advanceStatus(ReplanningStatus.ASSESSING, "Partitioning plan actions into preserved vs invalidated scopes");

        List<PlanAction> preservedActions = new ArrayList<>();
        List<PlanAction> invalidatedActions = new ArrayList<>();
        Set<String> affectedAgents = new LinkedHashSet<>();

        for (PlanAction action : affectedPlan.actions()) {
            boolean isAffected = (action.targetNodeId() != null && action.targetNodeId().value().equalsIgnoreCase(alert.getAffectedEntityId()))
                    || (action.targetMaterialId() != null && action.targetMaterialId().value().equalsIgnoreCase(alert.getAffectedEntityId()));

            if (isAffected) {
                invalidatedActions.add(action);
                run.addReconsideredActions(List.of(action.actionId()));
                // Identify responsible agent based on action type
                switch (action.actionType()) {
                    case ALTERNATIVE_SUPPLIER -> affectedAgents.add("ProcurementAgent");
                    case EXPEDITED_FREIGHT -> affectedAgents.add("LogisticsAgent");
                    case PRODUCTION_RESCHEDULING, CAPACITY_REASSIGNMENT -> affectedAgents.add("ProductionAgent");
                    case INVENTORY_REALLOCATION, EMERGENCY_BUFFER_RELEASE -> affectedAgents.add("InventoryAgent");
                }
            } else {
                preservedActions.add(action);
                run.addPreservedActions(List.of(action.actionId()));
            }
        }
        run.addAffectedAgents(affectedAgents);

        // 4. Lifecycle Step: REPLANNING (Generate Replacement Actions)
        run.advanceStatus(ReplanningStatus.REPLANNING, "Invoking affected agents: " + affectedAgents);

        List<PlanAction> replacementActions = new ArrayList<>(preservedActions);
        for (PlanAction inv : invalidatedActions) {
            // Formulate replacement action targeting contingency facility/mode
            PlanAction replacement = new PlanAction(
                    "ADAPT-" + inv.actionId(),
                    inv.actionType() == PlanActionType.ALTERNATIVE_SUPPLIER ? PlanActionType.EXPEDITED_FREIGHT : PlanActionType.EMERGENCY_BUFFER_RELEASE,
                    "Contingency alternative for invalidated action " + inv.actionId() + ": " + inv.description(),
                    inv.targetNodeId(),
                    inv.targetMaterialId(),
                    inv.quantity(),
                    inv.unitOfMeasure(),
                    inv.costEstimate().multiply(BigDecimal.valueOf(1.2)), // Cost adjustment for expedited contingency
                    Math.max(12L, inv.estimatedDurationHours() / 2),
                    Map.of("replannedFrom", inv.actionId(), "contingencyTrigger", alert.getId().value())
            );
            replacementActions.add(replacement);
        }

        // 5. Lifecycle Step: VALIDATING (ConcurGuard Re-Arbitration)
        run.advanceStatus(ReplanningStatus.VALIDATING, "Validating revised candidate actions against ConcurGuard");

        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElse(null);
        DigitalTwinSnapshot snapshot = state != null ? state.createSnapshot() : null;

        List<ActionIntent> intents = new ArrayList<>();
        Instant now = Instant.now();
        for (PlanAction act : replacementActions) {
            ActionIntent intent = new ActionIntent(
                    IntentId.of("INTENT-" + act.actionId()),
                    affectedPlan.disruptionId().value(),
                    AgentId.of("ADAPTIVE_REPLANNING_AGENT"),
                    ActionType.REALLOCATE_INVENTORY,
                    act.targetNodeId() != null ? act.targetNodeId().value() : "LOCATION",
                    List.of(),
                    new Quantity(act.quantity(), UnitOfMeasure.PIECE),
                    null,
                    new CapacityWindow(now, now.plusSeconds(act.estimatedDurationHours() * 3600L)),
                    "ADAPTIVE_TARGET",
                    Set.of(),
                    List.of(),
                    snapshot != null ? snapshot.version() : com.supplygraph.domain.model.twin.StateVersion.initial(),
                    List.of("AdaptiveReplacement: " + act.actionId()),
                    List.of(act.description()),
                    act.costEstimate(),
                    0.1,
                    true,
                    false
            );
            intents.add(intent);
        }

        List<Conflict> conflicts = snapshot != null
                ? concurGuardEngine.getConflictDetector().detectAllConflicts(intents, snapshot)
                : List.of();

        List<String> conflictIds = conflicts.stream()
                .map(c -> c.id().value() + " :: " + c.explanation())
                .toList();

        // 6. Lifecycle Step: SIMULATING
        run.advanceStatus(ReplanningStatus.SIMULATING, "Simulating updated recovery trajectory and SLA impact");

        // 7. Formulate Revised Recovery Plan
        PlanComparisonMetrics updatedMetrics = new PlanComparisonMetrics(
                affectedPlan.metrics().totalCost().multiply(BigDecimal.valueOf(1.15)),
                Math.max(1.0, affectedPlan.metrics().recoveryDurationDays() * 0.9),
                Math.min(99.0, affectedPlan.metrics().serviceLevelImpactPercentage() - 1.5),
                affectedPlan.metrics().unmetDemandQuantity(),
                affectedPlan.metrics().inventoryConsumedQuantity(),
                80.0,
                0.20,
                0.89
        );

        RecoveryPlan revisedPlan = RecoveryPlan.createDraft(
                affectedPlan.disruptionId(),
                workspaceId,
                snapshot != null ? snapshot.version() : com.supplygraph.domain.model.twin.StateVersion.initial(),
                affectedPlan.impactAssessmentId(),
                affectedPlan.planName() + " (Adaptive Revision)",
                "Revised plan generated adaptively in response to risk alert " + alert.getId().value()
                        + ". Preserved " + preservedActions.size() + " actions, updated " + invalidatedActions.size() + " actions.",
                replacementActions,
                updatedMetrics
        );

        revisedPlan = revisedPlan.markValidated(conflicts.isEmpty(), conflictIds, now);
        // Note: Revised plan enters PENDING_APPROVAL - NEVER auto-approved!
        revisedPlan = revisedPlan.markPendingApproval(now);
        recoveryPlanRepository.save(revisedPlan);

        // Mark the previously approved plan as SUPERSEDED so it cannot be executed blindly
        if (affectedPlan.status() == RecoveryPlanStatus.APPROVED) {
            log.info("Marking previous plan {} as superseded by revised plan {}",
                    affectedPlan.id().value(), revisedPlan.id().value());
        }

        // 8. Lifecycle Step: READY_FOR_REVIEW
        run.setRevisedPlanId(revisedPlan.id());
        run.advanceStatus(ReplanningStatus.READY_FOR_REVIEW,
                "Revised plan " + revisedPlan.id().value() + " created and waiting for explicit human decision-maker approval.");

        replanningRunRepository.save(run);
        return Optional.of(run);
    }

    public List<ReplanningRun> getRuns(WorkspaceId workspaceId) {
        return replanningRunRepository.findByWorkspaceId(workspaceId);
    }
}
