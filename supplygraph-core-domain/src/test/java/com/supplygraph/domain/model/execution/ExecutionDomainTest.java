package com.supplygraph.domain.model.execution;

import com.supplygraph.domain.model.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class ExecutionDomainTest {
    private static ExecutionTask t(int seq, TaskType type, String... deps) {
        return ExecutionTask.create("W1", seq, type, "t" + seq, Map.of(), Arrays.asList(deps), Map.of(), 2);
    }
    private static ExecutionWorkflow wf(ExecutionTask... tasks) {
        return ExecutionWorkflow.draft(WorkspaceId.of("ws"), "PLAN-1", "creator", "planApprover", "V1", List.of(tasks), null, 0);
    }
    private static ExecutionWorkflow approved(ExecutionWorkflow w) {
        w = w.validate().submitForApproval();
        w = w.addApproval(new WorkflowApproval(w.id(), "approver", "ADMIN", WorkflowApproval.Scope.WORKFLOW, true, "ok", Instant.now()));
        if (w.hasSensitiveTasks()) w = w.addApproval(new WorkflowApproval(w.id(), "approver2", "ADMIN", WorkflowApproval.Scope.SENSITIVE_TASKS, true, "ok", Instant.now()));
        return w.markApproved(true);
    }

    // --- graph
    @Test void topologicalOrderRespectsDependencies() {
        TaskGraph g = new TaskGraph(List.of(t(3, TaskType.NOTIFY_STAKEHOLDERS, "W1-T1", "W1-T2"), t(1, TaskType.ALLOCATE_INVENTORY), t(2, TaskType.REROUTE_SHIPMENT, "W1-T1")));
        assertThat(g.topologicalOrder()).containsExactly("W1-T1", "W1-T2", "W1-T3");
    }

    @Test void cycleIsDetectedAndNamed() {
        TaskGraph g = new TaskGraph(List.of(t(1, TaskType.ALLOCATE_INVENTORY, "W1-T2"), t(2, TaskType.REROUTE_SHIPMENT, "W1-T1"), t(3, TaskType.NOTIFY_STAKEHOLDERS)));
        assertThatThrownBy(g::topologicalOrder).isInstanceOf(TaskGraph.CycleException.class)
                .satisfies(e -> assertThat(((TaskGraph.CycleException) e).members()).containsExactlyInAnyOrder("W1-T1", "W1-T2"));
        assertThatThrownBy(() -> new TaskGraph(List.of(t(1, TaskType.ALLOCATE_INVENTORY, "W1-T1")))).isInstanceOf(TaskGraph.CycleException.class);
    }

    @Test void unknownDependencyAndDuplicateIdsRejected() {
        assertThatThrownBy(() -> new TaskGraph(List.of(t(1, TaskType.ALLOCATE_INVENTORY, "W1-T9")))).hasMessageContaining("unknown");
        assertThatThrownBy(() -> new TaskGraph(List.of(t(1, TaskType.ALLOCATE_INVENTORY), t(1, TaskType.REROUTE_SHIPMENT)))).hasMessageContaining("Duplicate");
    }

    @Test void downstreamTasksOnlyRunAfterPredecessorsComplete() {
        ExecutionTask a = t(1, TaskType.REQUEST_SUPPLIER_CAPACITY_CONFIRMATION), b = t(2, TaskType.EXPEDITE_PURCHASE_ORDER, "W1-T1"), c = t(3, TaskType.ALLOCATE_INVENTORY);
        assertThat(new TaskGraph(List.of(a, b, c)).runnable()).extracting(ExecutionTask::id).containsExactly("W1-T1", "W1-T3");
        ExecutionTask aDone = a.markReady().startAttempt().complete("ref", false);
        assertThat(new TaskGraph(List.of(aDone, b, c)).runnable()).extracting(ExecutionTask::id).containsExactly("W1-T2", "W1-T3");
        ExecutionTask aAck = a.markReady().startAttempt().awaitAck("ref");      // acknowledged is NOT completed
        assertThat(new TaskGraph(List.of(aAck, b, c)).runnable()).extracting(ExecutionTask::id).containsExactly("W1-T3");
    }

    @Test void failurePropagatesBlockingTransitively() {
        ExecutionTask a = t(1, TaskType.ALLOCATE_INVENTORY).markReady().startAttempt().failPermanently("no stock");
        ExecutionTask b = t(2, TaskType.REROUTE_SHIPMENT, "W1-T1"), c = t(3, TaskType.NOTIFY_STAKEHOLDERS, "W1-T2");
        TaskGraph g = new TaskGraph(List.of(a, b, c));
        assertThat(g.toBlock()).extracting(ExecutionTask::id).containsExactly("W1-T2");
        ExecutionTask bBlocked = b.markBlocked("upstream failed");
        assertThat(new TaskGraph(List.of(a, bBlocked, c)).toBlock()).extracting(ExecutionTask::id).containsExactly("W1-T3");
        assertThat(g.downstreamOf("W1-T1")).containsExactlyInAnyOrder("W1-T2", "W1-T3");
    }

    @Test void conditionalTaskRunsOnlyWhenConditionOutcomeMatchesElseSkipped() {
        ExecutionTask primary = t(1, TaskType.REQUEST_ALTERNATE_QUOTATION);
        ExecutionTask fallback = t(2, TaskType.EXPEDITE_PURCHASE_ORDER, "W1-T1").withCondition("W1-T1", TaskStatus.FAILED);
        ExecutionTask failed = primary.markReady().startAttempt().failPermanently("x");
        assertThat(new TaskGraph(List.of(failed, fallback)).runnable()).extracting(ExecutionTask::id).containsExactly("W1-T2");
        assertThat(new TaskGraph(List.of(failed, fallback)).toBlock()).isEmpty();
        ExecutionTask done = primary.markReady().startAttempt().complete("r", false);
        assertThat(new TaskGraph(List.of(done, fallback)).runnable()).isEmpty();
        assertThat(new TaskGraph(List.of(done, fallback)).skippable()).extracting(ExecutionTask::id).containsExactly("W1-T2");
    }

    // --- task state machine
    @Test void retryableFailuresAreBoundedThenPermanent() {
        ExecutionTask x = t(1, TaskType.ALLOCATE_INVENTORY).markReady().startAttempt().failRetryable("e1");
        assertThat(x.status()).isEqualTo(TaskStatus.RETRY_PENDING);
        x = x.startAttempt().failRetryable("e2");
        assertThat(x.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(x.attempts()).isEqualTo(2);
    }

    @Test void invalidTaskTransitionsRejected() {
        assertThatThrownBy(() -> t(1, TaskType.ALLOCATE_INVENTORY).complete("r", false)).isInstanceOf(IllegalStateException.class);
        ExecutionTask failed = t(1, TaskType.ALLOCATE_INVENTORY).markReady().startAttempt().failPermanently("x");
        assertThatThrownBy(failed::markReady).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> t(1, TaskType.ALLOCATE_INVENTORY).compensated()).isInstanceOf(IllegalStateException.class);
    }

    @Test void unknownOutcomeCannotBeRetriedDirectlyButCanBeReopenedAfterReconciliation() {
        ExecutionTask u = t(1, TaskType.ALLOCATE_INVENTORY).markReady().startAttempt().needsReconciliation("timeout");
        assertThatThrownBy(u::startAttempt).isInstanceOf(IllegalStateException.class);
        assertThat(u.reopenAfterReconciliation().status()).isEqualTo(TaskStatus.READY);
        ExecutionTask exhausted = u.reopenAfterReconciliation().startAttempt().needsReconciliation("again");
        assertThatThrownBy(exhausted::reopenAfterReconciliation).hasMessageContaining("budget");
    }

    // --- workflow state machine
    @Test void workflowFollowsHappyPathAndRejectsSkippingStates() {
        ExecutionWorkflow draft = wf(t(1, TaskType.ALLOCATE_INVENTORY));
        assertThatThrownBy(draft::submitForApproval).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> draft.markReady(true)).isInstanceOf(IllegalStateException.class);
        ExecutionWorkflow w = approved(draft).markReady(true).markExecuting();
        assertThat(w.status()).isEqualTo(WorkflowStatus.EXECUTING);
    }

    @Test void sensitiveTasksRequireSecondApproval() {
        ExecutionWorkflow w = wf(t(1, TaskType.EXPEDITE_PURCHASE_ORDER)).validate().submitForApproval();
        w = w.addApproval(new WorkflowApproval(w.id(), "approver", "ADMIN", WorkflowApproval.Scope.WORKFLOW, true, "ok", Instant.now()));
        ExecutionWorkflow pending = w;
        assertThatThrownBy(() -> pending.markApproved(true)).hasMessageContaining("additional approval");
        assertThat(pending.markApproved(false).status()).isEqualTo(WorkflowStatus.APPROVED);
    }

    @Test void rejectionIsTerminal() {
        ExecutionWorkflow w = wf(t(1, TaskType.ALLOCATE_INVENTORY)).validate().submitForApproval();
        w = w.addApproval(new WorkflowApproval(w.id(), "approver", "ADMIN", WorkflowApproval.Scope.WORKFLOW, false, "no", Instant.now()));
        assertThat(w.status()).isEqualTo(WorkflowStatus.REJECTED);
        assertThat(w.status().isTerminal()).isTrue();
        ExecutionWorkflow rejected = w;
        assertThatThrownBy(() -> rejected.markApproved(false)).isInstanceOf(IllegalStateException.class);
    }

    @Test void cyclicWorkflowCannotBeValidated() {
        assertThatThrownBy(() -> wf(t(1, TaskType.ALLOCATE_INVENTORY, "W1-T2"), t(2, TaskType.REROUTE_SHIPMENT, "W1-T1")).validate()).isInstanceOf(TaskGraph.CycleException.class);
    }

    @Test void settleDerivesCompletedPartialFailedReconciliationAndCompensationStatuses() {
        ExecutionWorkflow base = approved(wf(t(1, TaskType.ALLOCATE_INVENTORY), t(2, TaskType.REROUTE_SHIPMENT))).markReady(true).markExecuting();
        ExecutionTask done1 = base.tasks().get(0).markReady().startAttempt().complete("r", true);
        ExecutionTask done2 = base.tasks().get(1).markReady().startAttempt().complete("r", true);
        assertThat(base.withTasks(List.of(done1, done2)).settle().status()).isEqualTo(WorkflowStatus.COMPLETED);

        ExecutionTask fail2 = base.tasks().get(1).markReady().startAttempt().failPermanently("x");
        // ALLOCATE_INVENTORY is compensable -> compensation required; reroute-only completion is not compensable -> partial
        assertThat(base.withTasks(List.of(done1, fail2)).settle().status()).isEqualTo(WorkflowStatus.COMPENSATION_REQUIRED);
        ExecutionTask fail1 = base.tasks().get(0).markReady().startAttempt().failPermanently("x");
        assertThat(base.withTasks(List.of(fail1, done2)).settle().status()).isEqualTo(WorkflowStatus.PARTIALLY_COMPLETED);
        assertThat(base.withTasks(List.of(fail1, fail2)).settle().status()).isEqualTo(WorkflowStatus.FAILED);

        ExecutionTask unknown = base.tasks().get(1).markReady().startAttempt().needsReconciliation("timeout");
        assertThat(base.withTasks(List.of(done1, unknown)).settle().status()).isEqualTo(WorkflowStatus.RECONCILIATION_REQUIRED);
        assertThat(base.settle().status()).isEqualTo(WorkflowStatus.EXECUTING);   // pending work remains
    }

    @Test void cancelKeepsInFlightAndCompletedTasksUntouched() {
        ExecutionWorkflow w = approved(wf(t(1, TaskType.ALLOCATE_INVENTORY), t(2, TaskType.REROUTE_SHIPMENT))).markReady(true).markExecuting();
        ExecutionTask ack = w.tasks().get(0).markReady().startAttempt().awaitAck("ref");
        ExecutionWorkflow c = w.withTasks(List.of(ack, w.tasks().get(1))).cancel("operator");
        assertThat(c.status()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(c.tasks().get(0).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(c.tasks().get(1).status()).isEqualTo(TaskStatus.CANCELLED);
    }

    // --- task type semantics
    @Test void compensationOnlyWhereValid() {
        assertThat(TaskType.ALLOCATE_INVENTORY.compensable()).isTrue();
        assertThat(TaskType.REROUTE_SHIPMENT.compensable()).isFalse();
        assertThat(TaskType.NOTIFY_STAKEHOLDERS.compensable()).isFalse();
        assertThat(TaskType.INITIATE_APPROVED_MATERIAL_SUBSTITUTION.compensable()).isFalse();
        assertThat(TaskType.EXPEDITE_PURCHASE_ORDER.sensitive()).isTrue();
    }

    // --- closed loop
    @Test void unknownAndSimulatedActualsAreNeverTreatedAsRealDeviation() {
        Map<OutcomeMetric, Double> expected = Map.of(OutcomeMetric.TOTAL_COST, 100.0, OutcomeMetric.RECOVERY_DURATION_DAYS, 10.0, OutcomeMetric.SERVICE_LEVEL_PERCENT, 95.0);
        PlanOutcomeComparison none = PlanOutcomeComparison.compare(expected, List.of());
        assertThat(none.rows()).allSatisfy(r -> assertThat(r.status()).isEqualTo(PlanOutcomeComparison.Status.UNKNOWN));
        assertThat(none.isMaterial(15)).isFalse();

        ActualMeasurement sim = new ActualMeasurement("W1", OutcomeMetric.TOTAL_COST, 500, "sandbox", true, "u", Instant.now());
        PlanOutcomeComparison simOnly = PlanOutcomeComparison.compare(expected, List.of(sim));
        assertThat(simOnly.rows().stream().filter(r -> r.metric() == OutcomeMetric.TOTAL_COST).findFirst().orElseThrow().status()).isEqualTo(PlanOutcomeComparison.Status.SIMULATED);
        assertThat(simOnly.isMaterial(15)).isFalse();

        ActualMeasurement real = new ActualMeasurement("W1", OutcomeMetric.TOTAL_COST, 130, "ERP invoice", false, "u", Instant.now());
        PlanOutcomeComparison measured = PlanOutcomeComparison.compare(expected, List.of(real));
        assertThat(measured.maxMeasuredDeviationPercent().orElseThrow()).isEqualTo(30.0);
        assertThat(measured.isMaterial(15)).isTrue();
        assertThat(measured.isMaterial(40)).isFalse();
    }

    @Test void governorBoundsReplanning() {
        ExecutionWorkflow w = wf(t(1, TaskType.ALLOCATE_INVENTORY));
        ExecutionPolicy policy = ExecutionPolicy.defaults(WorkspaceId.of("ws"));
        Map<OutcomeMetric, Double> expected = Map.of(OutcomeMetric.TOTAL_COST, 100.0);
        PlanOutcomeComparison material = PlanOutcomeComparison.compare(expected, List.of(new ActualMeasurement("W1", OutcomeMetric.TOTAL_COST, 150, "erp", false, "u", Instant.now())));
        PlanOutcomeComparison fine = PlanOutcomeComparison.compare(expected, List.of(new ActualMeasurement("W1", OutcomeMetric.TOTAL_COST, 101, "erp", false, "u", Instant.now())));
        assertThat(ReassessmentGovernor.decide(w, fine, policy, false)).isEqualTo(ReassessmentGovernor.Decision.NOT_NEEDED);
        assertThat(ReassessmentGovernor.decide(w, material, policy, false)).isEqualTo(ReassessmentGovernor.Decision.PROPOSE);
        assertThat(ReassessmentGovernor.decide(w, material, policy, true)).isEqualTo(ReassessmentGovernor.Decision.SUPPRESSED_DUPLICATE);
        ExecutionWorkflow deep = new ExecutionWorkflow(w.id(), w.workspaceId(), w.planId(), w.correlationId(), w.status(), "c", "a", "V1", true, 2, null, w.tasks(), List.of(), Instant.now(), Instant.now(), 0);
        assertThat(ReassessmentGovernor.decide(deep, material, policy, false)).isEqualTo(ReassessmentGovernor.Decision.LIMIT_REACHED);
    }

    @Test void reassessmentRequestNeedsHumanDecisionWithRationaleAndDecidesOnce() {
        ExecutionWorkflow w = wf(t(1, TaskType.ALLOCATE_INVENTORY));
        ReassessmentRequest r = ReassessmentRequest.propose(w, 30, "cost overrun", "SYSTEM");
        assertThat(r.depth()).isEqualTo(1);
        assertThatThrownBy(() -> r.decide(true, "u", " ")).isInstanceOf(IllegalArgumentException.class);
        ReassessmentRequest d = r.decide(true, "u", "go");
        assertThat(d.status()).isEqualTo(ReassessmentRequest.Status.APPROVED);
        assertThatThrownBy(() -> d.decide(false, "u", "x")).isInstanceOf(IllegalStateException.class);
    }

    @Test void executionPolicyBoundsValidated() {
        WorkspaceId ws = WorkspaceId.of("ws");
        assertThatThrownBy(() -> new ExecutionPolicy(ws, true, true, true, false, 0, 2, 15)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExecutionPolicy(ws, true, true, true, false, 3, 9, 15)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExecutionPolicy(ws, true, true, true, false, 3, 2, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ExecutionPolicy.defaults(ws).allowLiveExecution()).isFalse();
    }
}
