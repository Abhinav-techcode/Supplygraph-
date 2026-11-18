package com.supplygraph.application.execution;

import com.supplygraph.application.execution.ExecutionAdapter.ExecutionResponse;
import com.supplygraph.application.execution.ExecutionAdapter.Outcome;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExecutionEngineTest {
    @AfterEach void tearDown() { SecurityContext.clear(); }

    private static ExecutionTask task(ExecutionWorkflow w, int n) { return w.tasks().get(n - 1); }
    private static String id(ExecutionWorkflow w, int n) { return w.tasks().get(n - 1).id(); }
    private ExecutionWorkflow reload(ExecutionFixture f, String id) { return f.workflows.get(id); }

    @Test void taskOrderAndDependenciesAreEnforcedAndAcknowledgementIsNotCompletion() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        // T1 (capacity confirmation) and T3 (inventory) are independent and ran; T2 (expedite) must wait for T1 to COMPLETE, not merely be acknowledged
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(task(w, 3).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(task(w, 2).status()).isEqualTo(TaskStatus.PENDING);
        assertThat(task(w, 4).status()).isEqualTo(TaskStatus.PENDING);
        assertThat(w.status()).isEqualTo(WorkflowStatus.EXECUTING);
        assertThat(f.recording.calls).extracting(ExecutionAdapter.Request::taskId).containsExactly(id(w, 1), id(w, 3));

        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThat(f.acks.record(id(w, 1), "REF-1", true, "sim-erp", "ok")).isEqualTo(ExecutionAcknowledgementService.Result.APPLIED);
        w = reload(f, w.id());
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task(w, 2).status()).isEqualTo(TaskStatus.AWAITING_ACK);       // released by the completed predecessor
        assertThat(task(w, 4).status()).isEqualTo(TaskStatus.PENDING);            // still waiting for T2 and T3

        f.acks.record(id(w, 2), "REF-2", true, "sim-erp", null);
        f.acks.record(id(w, 3), "REF-3", true, "sim-wms", null);
        w = reload(f, w.id());
        assertThat(task(w, 4).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        f.acks.record(id(w, 4), "REF-4", true, "sim-notify", null);
        w = reload(f, w.id());
        assertThat(w.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(w.simulated()).isTrue();
        assertThat(task(w, 1).completedWithSimulatedEvidence()).isTrue();
        verifyNoInteractions(f.ingestion);                                         // simulated evidence never reaches the digital twin
    }

    @Test void executorMustDifferFromApproversAndHoldPermission() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        String id = w.id();
        f.as(UserRole.WORKSPACE_ADMIN);                               // approver of this workflow
        assertThatThrownBy(() -> f.orchestration.start(id)).isInstanceOf(SeparationOfDutiesException.class);
        f.as(UserRole.ANALYST);
        assertThatThrownBy(() -> f.orchestration.start(id)).isInstanceOf(UnauthorizedOperationException.class);
        f.as(UserRole.VIEWER);
        assertThatThrownBy(() -> f.orchestration.start(id)).isInstanceOf(UnauthorizedOperationException.class);
        assertThat(f.recording.calls).isEmpty();
        assertThat(reload(f, id).status()).isEqualTo(WorkflowStatus.READY);
    }

    @Test void approvalCannotBeBypassedByStartingAnUnapprovedWorkflow() {
        ExecutionFixture f = new ExecutionFixture(true);
        var plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        f.as(UserRole.LOGISTICS_MANAGER);
        assertThatThrownBy(() -> f.orchestration.start(w.id())).hasMessageContaining("READY");
        assertThat(f.recording.calls).isEmpty();
    }

    @Test void retryableFailureWaitsForExplicitBoundedRetryWithStableIdempotencyKey() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.recording.script.put(id(w, 1), ExecutionResponse.of(Outcome.RETRYABLE_ERROR, null, "503"));
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.RETRY_PENDING);
        assertThat(task(w, 2).status()).isEqualTo(TaskStatus.PENDING);
        assertThat(w.status()).isEqualTo(WorkflowStatus.EXECUTING);
        String t1 = id(w, 1);

        f.recording.script.put(t1, ExecutionResponse.of(Outcome.ACKNOWLEDGED, "REF-OK", "accepted"));
        w = f.orchestration.retryTask(w.id(), t1);
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(task(w, 1).attempts()).isEqualTo(2);
        assertThat(f.recording.calls.stream().filter(c -> c.taskId().equals(t1)).map(ExecutionAdapter.Request::idempotencyKey).distinct()).hasSize(1);
        assertThat(f.attemptStore.stream().filter(a -> a.taskId().equals(t1)).map(ExecutionAttempt::outcome)).containsExactly(AttemptOutcome.RETRYABLE_FAILURE, AttemptOutcome.ACKNOWLEDGED);
    }

    @Test void retriesAreBoundedAndFailurePropagatesToDownstreamTasks() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        String t1 = id(w, 1), wid = w.id();
        f.recording.script.put(t1, ExecutionResponse.of(Outcome.RETRYABLE_ERROR, null, "503"));
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(wid);
        w = f.orchestration.retryTask(wid, t1);
        w = f.orchestration.retryTask(wid, t1);          // third attempt exhausts the budget (default 3)
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task(w, 1).attempts()).isEqualTo(3);
        assertThat(task(w, 2).status()).isEqualTo(TaskStatus.BLOCKED);
        assertThat(task(w, 4).status()).isEqualTo(TaskStatus.BLOCKED);
        assertThatThrownBy(() -> f.orchestration.retryTask(wid, t1)).hasMessageContaining("RETRY_PENDING");
        // inventory allocation (T3) was independent and is still awaiting its acknowledgement -> workflow keeps executing
        assertThat(w.status()).isEqualTo(WorkflowStatus.EXECUTING);
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        f.acks.record(id(w, 3), "REF-3", true, "sim-wms", null);
        w = reload(f, wid);
        assertThat(w.status()).isEqualTo(WorkflowStatus.COMPENSATION_REQUIRED);      // partial result including a compensable completed task
    }

    @Test void permanentFailureRejectionAndAuthFailureAreNotRetried() {
        for (Outcome o : List.of(Outcome.PERMANENT_ERROR, Outcome.REJECTED, Outcome.AUTHENTICATION_FAILURE)) {
            ExecutionFixture f = new ExecutionFixture(true);
            ExecutionWorkflow w = f.readyWorkflow();
            f.recording.script.put(id(w, 1), ExecutionResponse.of(o, null, "no"));
            f.as(UserRole.LOGISTICS_MANAGER);
            w = f.orchestration.start(w.id());
            assertThat(task(w, 1).status()).as(o.name()).isEqualTo(TaskStatus.FAILED);
            assertThat(task(w, 1).attempts()).isEqualTo(1);
            assertThat(task(w, 2).status()).isEqualTo(TaskStatus.BLOCKED);
        }
    }

    @Test void unknownOutcomeIsNeverRetriedBlindlyAndRequiresReconciliation() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        String t1 = id(w, 1), wid = w.id();
        f.recording.script.put(t1, ExecutionResponse.of(Outcome.TIMEOUT_UNKNOWN, null, "no response"));
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(wid);
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);
        assertThatThrownBy(() -> f.orchestration.retryTask(wid, t1)).hasMessageContaining("reconciled");
        assertThat(task(w, 2).status()).isEqualTo(TaskStatus.PENDING);               // downstream waits; it is NOT blocked as failed
        assertThat(f.attemptStore.stream().filter(a -> a.taskId().equals(t1)).map(ExecutionAttempt::outcome)).containsExactly(AttemptOutcome.UNKNOWN_OUTCOME);

        // human confirms nothing happened -> task reopened and executed again (fresh attempt)
        f.recording.script.put(t1, ExecutionResponse.of(Outcome.ACKNOWLEDGED, "REF-NEW", "ok"));
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        w = f.reconciliation.reconcile(t1, ExecutionReconciliationService.Resolution.CONFIRMED_NOT_EXECUTED, "checked supplier portal: no request found", Map.of());
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(task(w, 1).attempts()).isEqualTo(2);
        assertThat(f.evidenceStore).anyMatch(e -> e.kind() == ExecutionEvidence.Kind.MANUAL_NOT_EXECUTED);
    }

    @Test void adapterCrashAndAdapterTimeoutBothBecomeUnknownOutcomes() {
        String[] firstTask = new String[1];
        ExecutionFixture.RecordingAdapter flaky = new ExecutionFixture.RecordingAdapter(true) {
            @Override public ExecutionResponse execute(Request r) {
                if (r.taskId().endsWith("-T1")) throw new IllegalStateException("boom");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return super.execute(r);
            }
        };
        ExecutionFixture f = new ExecutionFixture(true, flaky);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);   // adapter threw
        assertThat(task(w, 3).status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);   // adapter exceeded its timeout
        assertThat(task(w, 3).lastError()).contains("timed out");
        assertThat(w.status()).isEqualTo(WorkflowStatus.RECONCILIATION_REQUIRED);
    }

    @Test void duplicateAndLateAcknowledgementsDoNotDuplicateEffects() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        String t1 = id(w, 1);
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThat(f.acks.record(t1, "R1", true, "erp", null)).isEqualTo(ExecutionAcknowledgementService.Result.APPLIED);
        int callsAfterFirst = f.recording.calls.size();
        assertThat(f.acks.record(t1, "R1", true, "erp", null)).isEqualTo(ExecutionAcknowledgementService.Result.DUPLICATE);
        assertThat(f.acks.record(t1, "R1-other", true, "erp", null)).isEqualTo(ExecutionAcknowledgementService.Result.RECORDED_NO_TRANSITION);  // already COMPLETED
        assertThat(f.recording.calls).hasSize(callsAfterFirst);
        assertThat(task(reload(f, w.id()), 1).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(f.eventStore).anyMatch(e -> e.type().equals("LATE_ACKNOWLEDGEMENT"));
    }

    @Test void negativeAcknowledgementFailsTheTaskAndBlocksDownstream() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        f.acks.record(id(w, 1), "R1", false, "erp", "supplier declined");
        w = reload(f, w.id());
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task(w, 2).status()).isEqualTo(TaskStatus.BLOCKED);
    }

    @Test void acknowledgementRequiresPermissionAndWorkspace() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        String t1 = id(w, 1);
        f.as(UserRole.ANALYST);
        assertThatThrownBy(() -> f.acks.record(t1, "R", true, "erp", null)).isInstanceOf(UnauthorizedOperationException.class);
        f.as(UserRole.WORKSPACE_ADMIN, com.supplygraph.domain.model.WorkspaceId.of("elsewhere"));
        assertThatThrownBy(() -> f.acks.record(t1, "R", true, "erp", null)).isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    @Test void liveVerifiedOutcomeUpdatesTwinButSimulatedNeverDoes() {
        ExecutionFixture f = new ExecutionFixture(false);
        f.policyStore.put(f.ws, new ExecutionPolicy(f.ws, true, true, true, true, 3, 2, 15));
        ExecutionWorkflow w = f.readyWorkflow();
        assertThat(w.simulated()).isFalse();
        f.recording.script.put(id(w, 3), ExecutionResponse.of(Outcome.COMPLETED_CONFIRMED, "WMS-77", "allocated"));
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        assertThat(task(w, 3).status()).isEqualTo(TaskStatus.COMPLETED);
        verify(f.ingestion, times(1)).ingestEvent(any());
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        f.acks.record(id(w, 1), "ERP-1", true, "erp", null);
        verify(f.ingestion, times(2)).ingestEvent(any());
    }

    @Test void liveAdapterIsNeverUsedUnlessPolicyAllowsIt() {
        ExecutionFixture f = new ExecutionFixture(false);       // only a LIVE adapter exists, policy default forbids live execution
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        assertThat(f.recording.calls).isEmpty();
        assertThat(task(w, 1).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task(w, 1).lastError()).contains("No execution adapter");
    }

    @Test void observationMatchingExpectedCompletesTaskAndMismatchDemandsReconciliation() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        String t3 = id(w, 3);                 // ALLOCATE_INVENTORY expects quantity=50
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        w = f.reconciliation.recordObservation(t3, Map.of("quantity", "40"), "wms-count");
        assertThat(task(w, 3).status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);
        w = f.reconciliation.recordObservation(t3, Map.of("quantity", "50.00"), "wms-recount");
        assertThat(task(w, 3).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(f.evidenceStore.stream().filter(e -> e.taskId().equals(t3) && e.kind() == ExecutionEvidence.Kind.OBSERVED_STATE)).hasSize(2);
    }

    @Test void reconciliationNeedsRationaleAndPermission() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.recording.script.put(id(w, 1), ExecutionResponse.of(Outcome.TIMEOUT_UNKNOWN, null, "x"));
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        String t1 = id(w, 1);
        f.as(UserRole.LOGISTICS_MANAGER);
        assertThatThrownBy(() -> f.reconciliation.reconcile(t1, ExecutionReconciliationService.Resolution.CONFIRMED_COMPLETED, "ok", Map.of())).isInstanceOf(UnauthorizedOperationException.class);
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThatThrownBy(() -> f.reconciliation.reconcile(t1, ExecutionReconciliationService.Resolution.CONFIRMED_COMPLETED, " ", Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void restartRecoveryTurnsInterruptedTasksIntoReconciliationNotRetry() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        // simulate a crash mid-call: a task persisted as IN_PROGRESS
        ExecutionTask stuck = task(w, 4).markReady().startAttempt();
        f.workflows.put(w.id(), w.withTasks(w.tasks().stream().map(t -> t.id().equals(stuck.id()) ? stuck : t).toList()));
        int callsBefore = f.recording.calls.size();
        assertThat(f.orchestration.recoverInterrupted()).isEqualTo(1);
        ExecutionWorkflow after = reload(f, w.id());
        assertThat(task(after, 4).status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);
        assertThat(task(after, 4).lastError()).contains("restart");
        assertThat(f.recording.calls).hasSize(callsBefore);
        assertThat(after.status()).isEqualTo(WorkflowStatus.RECONCILIATION_REQUIRED);
    }

    @Test void compensationOnlyForCompensableCompletedTasks() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.recording.script.put(id(w, 1), ExecutionResponse.of(Outcome.PERMANENT_ERROR, null, "no"));
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        f.acks.record(id(w, 3), "R3", true, "wms", null);
        w = reload(f, w.id());
        assertThat(w.status()).isEqualTo(WorkflowStatus.COMPENSATION_REQUIRED);
        String t3 = id(w, 3), wid = w.id();
        assertThatThrownBy(() -> f.compensation.compensate(t3, "undo")).isInstanceOf(UnauthorizedOperationException.class);   // manager lacks EXECUTION_COMPENSATE
        f.as(UserRole.WORKSPACE_ADMIN);
        assertThatThrownBy(() -> f.compensation.compensate(id(reload(f, wid), 2), "undo")).hasMessageContaining("COMPLETED");
        w = f.compensation.compensate(t3, "plan failed upstream");
        assertThat(task(w, 3).status()).isEqualTo(TaskStatus.COMPENSATED);
        assertThat(f.recording.compensations).hasSize(1);
        assertThat(f.compensation.acceptPartial(wid, "operator accepts").status()).isEqualTo(WorkflowStatus.PARTIALLY_COMPLETED);
    }

    @Test void irreversibleTaskTypesCannotBeCompensated() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        ExecutionTask notify = task(w, 4).markReady().startAttempt().complete("r", true);
        f.workflows.put(w.id(), w.withTasks(w.tasks().stream().map(t -> t.id().equals(notify.id()) ? notify : t).toList()));
        f.as(UserRole.WORKSPACE_ADMIN);
        assertThatThrownBy(() -> f.compensation.compensate(notify.id(), "undo")).hasMessageContaining("No valid compensation");
        assertThat(f.recording.compensations).isEmpty();
    }

    @Test void cancellationRequiresReasonAndStopsFurtherExecution() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        String wid = w.id();
        assertThatThrownBy(() -> f.orchestration.cancel(wid, " ")).isInstanceOf(IllegalArgumentException.class);
        w = f.orchestration.cancel(wid, "supplier resolved the shortage");
        assertThat(w.status()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThatThrownBy(() -> f.orchestration.start(wid)).isInstanceOf(IllegalStateException.class);
        assertThat(f.recording.calls).isEmpty();
    }

    @Test void staleWorkflowIsBlockedAtStartNotExecuted() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.twinVersion = new com.supplygraph.domain.model.twin.StateVersion(f.twinVersion.value() + 1);
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        assertThat(w.status()).isEqualTo(WorkflowStatus.BLOCKED);
        assertThat(f.recording.calls).isEmpty();
    }

    @Test void timelineCapturesCorrelatedProvenance() {
        ExecutionFixture f = new ExecutionFixture(true);
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.LOGISTICS_MANAGER);
        w = f.orchestration.start(w.id());
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        String corr = w.correlationId();
        List<ExecutionEvent> tl = f.queries.timeline(w.id());
        assertThat(tl).isNotEmpty().allSatisfy(e -> { assertThat(e.correlationId()).isEqualTo(corr); assertThat(e.simulated()).isTrue(); });
        assertThat(tl).extracting(ExecutionEvent::type).contains("WORKFLOW_CREATED", "APPROVAL_RECORDED", "WORKFLOW_READY", "WORKFLOW_STARTED", "ATTEMPT_STARTED");
    }
}
