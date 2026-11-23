package com.supplygraph.application.execution;

import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.application.port.out.OutcomeRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.event.*;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.plan.PlanComparisonMetrics;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.security.Permission;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Closed loop: planned/approved expectation vs measured outcome, bounded governed reassessment, and feeding real measurements into monitoring.
 * Missing actuals stay UNKNOWN; simulated actuals stay SIMULATED and never trigger a reassessment.
 */
@Service
public class ClosedLoopService {
    private final ExecutionSupport support;
    private final OutcomeRepositoryPort outcomes;
    private final RecoveryPlanRepositoryPort plans;
    private final OperationalEventIngestionService events;

    public ClosedLoopService(ExecutionSupport support, OutcomeRepositoryPort outcomes, RecoveryPlanRepositoryPort plans, OperationalEventIngestionService events) {
        this.support = support; this.outcomes = outcomes; this.plans = plans; this.events = events;
    }

    public ActualMeasurement recordActual(String workflowId, OutcomeMetric metric, double value, String source, boolean simulated) {
        ExecutionWorkflow w = support.load(workflowId, Permission.EXECUTION_RECONCILE);
        boolean sim = simulated || w.simulated();    // a simulated workflow cannot produce real measurements
        ActualMeasurement saved = outcomes.saveActual(new ActualMeasurement(workflowId, metric, value, source, sim, support.actor(), Instant.now()));
        support.timeline(w, null, "ACTUAL_RECORDED", metric + "=" + value + " " + metric.unit() + " (" + source + ")" + (sim ? " [SIMULATED]" : ""));
        if (!sim) {
            events.ingestEvent(OperationalEvent.createIngested(EventType.RECOVERY_ACTION_COMPLETED, EventSource.MANUAL_OPERATOR, Instant.now(), w.id(), "RECOVERY_WORKFLOW", w.workspaceId(),
                    Map.of("metric", metric.name(), "actual", Double.toString(value), "workflowId", w.id(), "correlationId", w.correlationId()),
                    new EventProvenance("SUPPLYGRAPH_EXECUTION", support.actor(), w.id(), source, Instant.now()),
                    "actual:" + w.id() + ":" + metric + ":" + value));
        }
        return saved;
    }

    public PlanOutcomeComparison comparison(String workflowId) {
        ExecutionWorkflow w = support.load(workflowId, null);
        RecoveryPlan plan = plans.findById(RecoveryPlanId.of(w.planId())).orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + w.planId()));
        return PlanOutcomeComparison.compare(expected(plan.metrics()), outcomes.findActuals(workflowId));
    }

    /** Evaluates deviation and, under the governor, proposes at most one open reassessment. A human must decide it. */
    public Optional<ReassessmentRequest> evaluateDeviation(String workflowId) {
        ExecutionWorkflow w = support.load(workflowId, Permission.EXECUTION_RECONCILE);
        return support.withLock(workflowId, () -> {
            ExecutionPolicy policy = support.policy(w.workspaceId());
            PlanOutcomeComparison cmp = comparison(workflowId);
            boolean open = outcomes.findReassessmentsByWorkflow(workflowId).stream().anyMatch(r -> r.status() == ReassessmentRequest.Status.PROPOSED);
            ReassessmentGovernor.Decision d = ReassessmentGovernor.decide(w, cmp, policy, open);
            double dev = cmp.maxMeasuredDeviationPercent().orElse(0);
            switch (d) {
                case PROPOSE -> {
                    ReassessmentRequest r = outcomes.saveReassessment(ReassessmentRequest.propose(w, dev, String.format("Measured deviation of %.1f%% exceeds the %.1f%% threshold", dev, policy.deviationThresholdPercent()), "SYSTEM"));
                    support.timeline(w, null, "REASSESSMENT_PROPOSED", r.reason());
                    support.audit(w, AuditAction.REASSESSMENT_PROPOSED, r.id(), "NONE", "PROPOSED", r.reason());
                    return Optional.of(r);
                }
                case LIMIT_REACHED -> support.timeline(w, null, "REASSESSMENT_LIMIT_REACHED", "Replanning depth limit " + policy.maxReplanDepth() + " reached; escalate to a human decision-maker");
                case SUPPRESSED_DUPLICATE -> support.timeline(w, null, "REASSESSMENT_SUPPRESSED", "An open reassessment proposal already exists");
                case NOT_NEEDED -> { }
            }
            return Optional.empty();
        });
    }

    /** Human decision on a proposal. Approval is recorded and handed to the existing Recovery Decision Centre; no plan is auto-generated or auto-executed. */
    public ReassessmentRequest decide(String requestId, boolean approve, String rationale) {
        ReassessmentRequest r = outcomes.findReassessment(requestId).orElseThrow(() -> new ResourceNotFoundException("Reassessment request not found: " + requestId));
        support.guard().enforceWorkspaceAndPermission(r.workspaceId(), Permission.PLAN_APPROVE);
        ReassessmentRequest decided = outcomes.saveReassessment(r.decide(approve, support.actor(), rationale));
        ExecutionWorkflow w = support.reload(r.workflowId());
        support.timeline(w, null, "REASSESSMENT_" + decided.status(), rationale);
        support.audit(w, AuditAction.REASSESSMENT_DECIDED, requestId, "PROPOSED", decided.status().name(), rationale);
        return decided;
    }

    public List<ReassessmentRequest> requests(com.supplygraph.domain.model.WorkspaceId ws, int limit) {
        support.guard().enforceWorkspaceAccess(ws);
        return outcomes.findReassessmentsByWorkspace(ws, limit);
    }

    public List<ActualMeasurement> actuals(String workflowId) { support.load(workflowId, null); return outcomes.findActuals(workflowId); }

    /** Decision-to-completion latency from recorded timestamps; empty when the workflow has not completed. */
    public Optional<Duration> decisionToCompletionLatency(ExecutionWorkflow w, Instant planApprovedAt) {
        if (w.status() != WorkflowStatus.COMPLETED) return Optional.empty();
        return Optional.of(Duration.between(planApprovedAt, w.updatedAt()));
    }

    static Map<OutcomeMetric, Double> expected(PlanComparisonMetrics m) {
        Map<OutcomeMetric, Double> e = new EnumMap<>(OutcomeMetric.class);
        e.put(OutcomeMetric.RECOVERY_DURATION_DAYS, m.recoveryDurationDays());
        e.put(OutcomeMetric.TOTAL_COST, m.totalCost().doubleValue());
        e.put(OutcomeMetric.SERVICE_LEVEL_PERCENT, m.serviceLevelImpactPercentage());
        e.put(OutcomeMetric.UNMET_DEMAND_QUANTITY, m.unmetDemandQuantity().doubleValue());
        return e;
    }
}
