package com.supplygraph.application.execution;

import com.supplygraph.domain.error.InvalidStateException;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.security.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ExecutionConversionAndApprovalTest {
    private ExecutionFixture f;
    @BeforeEach void setUp() { f = new ExecutionFixture(true); }
    @AfterEach void tearDown() { SecurityContext.clear(); }

    @Test void approvedPlanBecomesValidatedWorkflowWithOrderedDependenciesAndProvenance() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        assertThat(w.status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);
        assertThat(w.createdBy()).isEqualTo("u-supply_chain_manager");
        assertThat(w.tasks()).extracting(ExecutionTask::type).containsExactly(TaskType.REQUEST_SUPPLIER_CAPACITY_CONFIRMATION, TaskType.EXPEDITE_PURCHASE_ORDER,
                TaskType.ALLOCATE_INVENTORY, TaskType.NOTIFY_STAKEHOLDERS);
        assertThat(w.tasks().get(1).dependsOn()).containsExactly(w.tasks().get(0).id());
        assertThat(w.tasks().get(3).dependsOn()).hasSize(3);
        assertThat(f.eventStore).anyMatch(e -> e.type().equals("WORKFLOW_CREATED"));
        org.mockito.Mockito.verify(f.audit).logAction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(f.ws), org.mockito.ArgumentMatchers.eq(com.supplygraph.domain.model.audit.AuditAction.WORKFLOW_CREATED),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test void conversionIsDeterministic() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        var a = WorkflowPlanner.plan("W", plan.actions(), 3);
        var b = WorkflowPlanner.plan("W", plan.actions(), 3);
        assertThat(a).usingRecursiveComparison().ignoringFields("updatedAt").isEqualTo(b);
    }

    @Test void unapprovedPlanCannotBeConverted() {
        RecoveryPlan approved = f.approvedPlan("u-supply_chain_manager");
        RecoveryPlan draft = RecoveryPlan.createDraft(approved.disruptionId(), f.ws, approved.sourceTwinVersion(), null, "d", "d", approved.actions(), null);
        f.planStore.put(draft.id().value(), draft);
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThatThrownBy(() -> f.conversion.convert(draft.id().value())).hasMessageContaining("APPROVED");
    }

    @Test void stalePlanProducesBlockedWorkflowAndBoundedReassessmentProposalNotExecution() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.twinVersion = new StateVersion(plan.sourceTwinVersion().value() + 5);
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        assertThat(w.status()).isEqualTo(WorkflowStatus.BLOCKED);
        assertThat(w.statusReason()).contains("Stale plan");
        assertThat(f.reassessments.values()).hasSize(1).allSatisfy(r -> assertThat(r.status()).isEqualTo(ReassessmentRequest.Status.PROPOSED));
        f.as(UserRole.WORKSPACE_ADMIN);
        assertThatThrownBy(() -> f.approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "x")).isInstanceOf(InvalidStateException.class);
        f.as(UserRole.LOGISTICS_MANAGER);
        assertThatThrownBy(() -> f.orchestration.start(w.id())).isInstanceOf(InvalidStateException.class);
        assertThat(f.recording.calls).isEmpty();
        // converting the same stale plan again must not stack up duplicate proposals
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        f.conversion.convert(plan.id().value());
        assertThat(f.reassessments).hasSize(1);
    }

    @Test void concurGuardConflictsBlockTheWorkflow() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.conflicts = List.of("CONFLICT-1 :: capacity double booked");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        assertThat(w.status()).isEqualTo(WorkflowStatus.BLOCKED);
        assertThat(w.statusReason()).contains("ConcurGuard");
    }

    @Test void approvalMustBeTraceableToAnAuthorizedUser() {
        RecoveryPlan ghost = f.approvedPlan("somebody-typed-in-a-text-box");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThat(f.conversion.convert(ghost.id().value()).statusReason()).contains("not traceable");
        RecoveryPlan viewerApproved = f.approvedPlan("u-viewer");
        assertThat(f.conversion.convert(viewerApproved.id().value()).statusReason()).contains("PLAN_APPROVE");
    }

    @Test void onlyOneActiveWorkflowPerPlan() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        f.conversion.convert(plan.id().value());
        assertThatThrownBy(() -> f.conversion.convert(plan.id().value())).hasMessageContaining("already exists");
    }

    @Test void conversionRequiresPermissionAndWorkspace() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.ANALYST);
        assertThatThrownBy(() -> f.conversion.convert(plan.id().value())).isInstanceOf(UnauthorizedOperationException.class);
        f.as(UserRole.WORKSPACE_ADMIN, com.supplygraph.domain.model.WorkspaceId.of("elsewhere"));
        assertThatThrownBy(() -> f.conversion.convert(plan.id().value())).isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    // ---- approvals / SoD
    @Test void creatorCannotApproveOwnWorkflow() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        assertThatThrownBy(() -> f.approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "self")).isInstanceOf(SeparationOfDutiesException.class);
        assertThat(f.workflows.get(w.id()).status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);
    }

    @Test void usersWithoutApprovePermissionCannotApprove() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        f.as(UserRole.LOGISTICS_MANAGER);
        assertThatThrownBy(() -> f.approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "x")).isInstanceOf(UnauthorizedOperationException.class);
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);   // has EXECUTION_APPROVE but is not allowed to give the sensitive approval
        assertThatThrownBy(() -> f.approvals.approve(w.id(), WorkflowApproval.Scope.SENSITIVE_TASKS, "x")).isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test void sensitiveTasksNeedAdistinctSecondApproverBeforeReady() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        f.as(UserRole.WORKSPACE_ADMIN);
        w = f.approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "first");
        assertThat(w.status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);      // not ready yet
        String id = w.id();
        assertThatThrownBy(() -> f.approvals.approve(id, WorkflowApproval.Scope.SENSITIVE_TASKS, "same person")).isInstanceOf(SeparationOfDutiesException.class);
        f.as(UserRole.SYSTEM_ADMIN);
        w = f.approvals.approve(id, WorkflowApproval.Scope.SENSITIVE_TASKS, "second");
        assertThat(w.status()).isEqualTo(WorkflowStatus.READY);
        assertThat(w.simulated()).isTrue();
    }

    @Test void rejectionEndsTheWorkflowAndPreventsExecution() {
        RecoveryPlan plan = f.approvedPlan("u-supply_chain_manager");
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = f.conversion.convert(plan.id().value());
        f.as(UserRole.WORKSPACE_ADMIN);
        assertThat(f.approvals.reject(w.id(), "too risky").status()).isEqualTo(WorkflowStatus.REJECTED);
        f.as(UserRole.LOGISTICS_MANAGER);
        assertThatThrownBy(() -> f.orchestration.start(w.id())).isInstanceOf(InvalidStateException.class);
    }

    @Test void workflowOfAnotherWorkspaceIsInvisibleAndImmutableToOutsiders() {
        ExecutionWorkflow w = f.readyWorkflow();
        f.as(UserRole.WORKSPACE_ADMIN, com.supplygraph.domain.model.WorkspaceId.of("elsewhere"));
        assertThatThrownBy(() -> f.queries.get(w.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> f.orchestration.start(w.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> f.approvals.reject(w.id(), "x")).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> f.queries.timeline(w.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    @Test void defaultPolicyIsTheSafeOneAndPolicyCanBeRelaxedByAdminOnly() {
        f.as(UserRole.WORKSPACE_ADMIN);
        assertThat(f.queries.policy(f.ws).approverMustDifferFromCreator()).isTrue();
        assertThat(f.queries.policy(f.ws).allowLiveExecution()).isFalse();
        f.queries.savePolicy(new ExecutionPolicy(f.ws, false, false, false, false, 3, 2, 15));
        assertThat(f.queries.policy(f.ws).approverMustDifferFromCreator()).isFalse();
        f.as(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThatThrownBy(() -> f.queries.savePolicy(ExecutionPolicy.defaults(f.ws))).isInstanceOf(UnauthorizedOperationException.class);
    }
}
