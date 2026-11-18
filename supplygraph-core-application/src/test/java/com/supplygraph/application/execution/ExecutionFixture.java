package com.supplygraph.application.execution;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.*;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.security.*;
import com.supplygraph.domain.model.twin.*;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** In-memory ports + real services wired together, so execution behaviour is tested end to end without a database. */
class ExecutionFixture {
    final WorkspaceId ws = WorkspaceId.of("ws-x");
    final Map<String, ExecutionWorkflow> workflows = new LinkedHashMap<>();
    final List<ExecutionAttempt> attemptStore = new ArrayList<>();
    final List<ExecutionEvidence> evidenceStore = new ArrayList<>();
    final List<ExecutionEvent> eventStore = new ArrayList<>();
    final List<ActualMeasurement> actualStore = new ArrayList<>();
    final Map<String, ReassessmentRequest> reassessments = new LinkedHashMap<>();
    final Map<WorkspaceId, ExecutionPolicy> policyStore = new HashMap<>();
    final Map<String, RecoveryPlan> planStore = new HashMap<>();
    final Map<String, UserProfile> userStore = new HashMap<>();
    StateVersion twinVersion = StateVersion.initial();
    List<String> conflicts = List.of();

    final OperationalEventIngestionService ingestion = mock(OperationalEventIngestionService.class);
    final RecoveryPlanningApplicationService planning = mock(RecoveryPlanningApplicationService.class);
    final ExecutionAdapter adapter;
    final RecordingAdapter recording;
    final ExecutionSupport support;
    final ExecutionOrchestrationService orchestration;
    final RecoveryPlanConversionService conversion;
    final ExecutionApprovalService approvals;
    final ExecutionAcknowledgementService acks;
    final ExecutionReconciliationService reconciliation;
    final ExecutionCompensationService compensation;
    final ClosedLoopService closedLoop;
    final ExecutionQueryService queries;
    final AuditGovernanceService audit = mock(AuditGovernanceService.class);

    /** Scriptable adapter: behaviour per task id, defaults to ACKNOWLEDGED; counts effects per idempotency key. */
    static class RecordingAdapter implements ExecutionAdapter {
        final boolean simulated;
        final Map<String, ExecutionResponse> script = new HashMap<>();
        final List<Request> calls = new ArrayList<>();
        final List<Request> compensations = new ArrayList<>();
        RecordingAdapter(boolean simulated) { this.simulated = simulated; }
        public String adapterId() { return simulated ? "test-sim" : "test-live"; }
        public boolean simulated() { return simulated; }
        public Set<TaskType> supportedTypes() { return EnumSet.allOf(TaskType.class); }
        public java.time.Duration timeout() { return java.time.Duration.ofMillis(500); }
        public List<String> checkPreconditions(Request r) { return List.of(); }
        public ExecutionResponse execute(Request r) {
            calls.add(r);
            return script.getOrDefault(r.taskId(), ExecutionResponse.of(Outcome.ACKNOWLEDGED, "REF-" + r.taskId(), "accepted"));
        }
        public ExecutionResponse compensate(Request r) { compensations.add(r); return ExecutionResponse.of(Outcome.ACKNOWLEDGED, "C-" + r.taskId(), "ok"); }
    }

    ExecutionFixture(boolean simulatedAdapter) { this(simulatedAdapter, null); }

    ExecutionFixture(boolean simulatedAdapter, RecordingAdapter custom) {
        recording = custom != null ? custom : new RecordingAdapter(simulatedAdapter);
        adapter = recording;
        ExecutionWorkflowRepositoryPort wp = new ExecutionWorkflowRepositoryPort() {
            public ExecutionWorkflow save(ExecutionWorkflow w) {
                ExecutionWorkflow cur = workflows.get(w.id());
                if (cur != null && w.version() < cur.version()) throw new IllegalStateException("Stale write rejected for " + w.id());
                workflows.put(w.id(), w); return w;
            }
            public Optional<ExecutionWorkflow> findById(String id) { return Optional.ofNullable(workflows.get(id)); }
            public List<ExecutionWorkflow> findByWorkspace(WorkspaceId s, int l) { return workflows.values().stream().filter(w -> w.workspaceId().equals(s)).toList(); }
            public List<ExecutionWorkflow> findByPlan(WorkspaceId s, String p) { return workflows.values().stream().filter(w -> w.workspaceId().equals(s) && w.planId().equals(p)).toList(); }
            public List<ExecutionWorkflow> findWithTaskStuck(TaskStatus st, Instant b) { return workflows.values().stream().filter(w -> w.tasks().stream().anyMatch(t -> t.status() == st)).toList(); }
            public List<ExecutionWorkflow> findByStatus(WorkflowStatus st) { return workflows.values().stream().filter(w -> w.status() == st).toList(); }
        };
        ExecutionAttemptRepositoryPort ap = new ExecutionAttemptRepositoryPort() {
            public ExecutionAttempt save(ExecutionAttempt a) { attemptStore.removeIf(x -> x.id().equals(a.id())); attemptStore.add(a); return a; }
            public List<ExecutionAttempt> attemptsByWorkflow(String id) { return attemptStore.stream().filter(a -> a.workflowId().equals(id)).toList(); }
            public List<ExecutionAttempt> attemptsByTask(String id) { return attemptStore.stream().filter(a -> a.taskId().equals(id)).toList(); }
        };
        ExecutionEvidenceRepositoryPort ep = new ExecutionEvidenceRepositoryPort() {
            public ExecutionEvidence save(ExecutionEvidence e) { evidenceStore.add(e); return e; }
            public List<ExecutionEvidence> evidenceByWorkflow(String id) { return evidenceStore.stream().filter(e -> e.workflowId().equals(id)).toList(); }
            public List<ExecutionEvidence> evidenceByTask(String id) { return evidenceStore.stream().filter(e -> e.taskId().equals(id)).toList(); }
            public boolean exists(String t, ExecutionEvidence.Kind k, String ref) { return evidenceStore.stream().anyMatch(e -> e.taskId().equals(t) && e.kind() == k && Objects.equals(e.reference(), ref)); }
        };
        ExecutionEventRepositoryPort evp = new ExecutionEventRepositoryPort() {
            public ExecutionEvent append(ExecutionEvent e) { eventStore.add(e); return e; }
            public List<ExecutionEvent> eventsByWorkflow(String id) { return eventStore.stream().filter(e -> e.workflowId().equals(id)).toList(); }
            public List<ExecutionEvent> eventsByWorkspace(WorkspaceId s, int l) { return eventStore.stream().filter(e -> e.workspaceId().equals(s)).toList(); }
        };
        ExecutionPolicyRepositoryPort pp = new ExecutionPolicyRepositoryPort() {
            public Optional<ExecutionPolicy> find(WorkspaceId s) { return Optional.ofNullable(policyStore.get(s)); }
            public ExecutionPolicy save(ExecutionPolicy p) { policyStore.put(p.workspaceId(), p); return p; }
        };
        OutcomeRepositoryPort op = new OutcomeRepositoryPort() {
            public ActualMeasurement saveActual(ActualMeasurement a) { actualStore.add(a); return a; }
            public List<ActualMeasurement> findActuals(String id) { return actualStore.stream().filter(a -> a.workflowId().equals(id)).toList(); }
            public ReassessmentRequest saveReassessment(ReassessmentRequest r) { reassessments.put(r.id(), r); return r; }
            public Optional<ReassessmentRequest> findReassessment(String id) { return Optional.ofNullable(reassessments.get(id)); }
            public List<ReassessmentRequest> findReassessmentsByWorkflow(String id) { return reassessments.values().stream().filter(r -> r.workflowId().equals(id)).toList(); }
            public List<ReassessmentRequest> findReassessmentsByWorkspace(WorkspaceId s, int l) { return reassessments.values().stream().filter(r -> r.workspaceId().equals(s)).toList(); }
        };
        RecoveryPlanRepositoryPort rp = mock(RecoveryPlanRepositoryPort.class);
        when(rp.findById(any())).thenAnswer(i -> Optional.ofNullable(planStore.get(i.<RecoveryPlanId>getArgument(0).value())));
        DigitalTwinRepositoryPort tp = mock(DigitalTwinRepositoryPort.class);
        DigitalTwinState state = mock(DigitalTwinState.class);
        when(state.getCurrentVersion()).thenAnswer(i -> twinVersion);
        when(tp.findByWorkspaceId(any())).thenReturn(Optional.of(state));
        UserProfileRepositoryPort up = mock(UserProfileRepositoryPort.class);
        when(up.findById(any())).thenAnswer(i -> Optional.ofNullable(userStore.get(i.<UserId>getArgument(0).value())));
        when(up.findByUsername(any())).thenAnswer(i -> userStore.values().stream().filter(u -> u.username().equals(i.getArgument(0))).findFirst());
        when(planning.revalidateConflicts(any())).thenAnswer(i -> conflicts);

        support = new ExecutionSupport(wp, evp, pp, new WorkspaceAccessGuard(), audit);
        AdapterSelector selector = new AdapterSelector(List.of(adapter));
        TwinFeedback feedback = new TwinFeedback(ingestion);
        orchestration = new ExecutionOrchestrationService(support, selector, ap, ep, tp, feedback);
        conversion = new RecoveryPlanConversionService(rp, tp, up, planning, wp, op, support);
        approvals = new ExecutionApprovalService(support, selector);
        acks = new ExecutionAcknowledgementService(support, orchestration, ep, ap, feedback);
        reconciliation = new ExecutionReconciliationService(support, orchestration, ep, ap, feedback);
        compensation = new ExecutionCompensationService(support, selector, ep);
        closedLoop = new ClosedLoopService(support, op, rp, ingestion);
        queries = new ExecutionQueryService(support, wp, ap, ep, evp, pp);
        for (UserRole r : UserRole.values()) {
            UserProfile u = UserProfile.create("u-" + r.name().toLowerCase(), "user-" + r.name().toLowerCase(), r.name() + "@x.io", r.name(), r, Set.of(ws));
            userStore.put(u.id().value(), u);
        }
    }

    void as(UserRole role) { SecurityContext.setCurrentUser(userStore.get("u-" + role.name().toLowerCase())); }
    void as(UserRole role, WorkspaceId other) {
        SecurityContext.setCurrentUser(UserProfile.create("x-" + role, "x", "x@x.io", "X", role, Set.of(other)));
    }

    /** Approved plan with a procurement chain (-> sensitive expedite) and an inventory allocation. */
    RecoveryPlan approvedPlan(String approverUserId) {
        List<PlanAction> actions = List.of(
                new PlanAction("A1", PlanActionType.PURCHASE_ORDER_EXPEDITE, "Expedite PO", null, null, new java.math.BigDecimal("100"), "PIECE", java.math.BigDecimal.TEN, 24, Map.of()),
                new PlanAction("A2", PlanActionType.INVENTORY_REALLOCATION, "Move stock", null, null, new java.math.BigDecimal("50"), "PIECE", java.math.BigDecimal.ONE, 12, Map.of()));
        PlanComparisonMetrics metrics = new PlanComparisonMetrics(new java.math.BigDecimal("1000"), 10.0, 95.0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 60, 0.1, 0.9);
        RecoveryPlan draft = RecoveryPlan.createDraft(DisruptionId.generate(), ws, twinVersion, null, "Plan", "d", actions, metrics);
        RecoveryPlan plan = draft.markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now()).approve(approverUserId, "ok", twinVersion, Instant.now());
        planStore.put(plan.id().value(), plan);
        return plan;
    }

    /** Creates, approves (admin) and returns a READY workflow for the standard plan. */
    ExecutionWorkflow readyWorkflow() {
        RecoveryPlan plan = approvedPlan("u-supply_chain_manager");
        as(UserRole.SUPPLY_CHAIN_MANAGER);
        ExecutionWorkflow w = conversion.convert(plan.id().value());
        as(UserRole.WORKSPACE_ADMIN);
        w = approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "approved");
        if (w.status() == WorkflowStatus.PENDING_APPROVAL) {
            as(UserRole.SYSTEM_ADMIN);
            w = approvals.approve(w.id(), WorkflowApproval.Scope.SENSITIVE_TASKS, "second approver");
        }
        return w;
    }
}
