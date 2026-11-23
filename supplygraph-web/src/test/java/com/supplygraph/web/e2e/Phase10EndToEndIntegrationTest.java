package com.supplygraph.web.e2e;

import com.supplygraph.adapter.integration.SandboxExecutionAdapter;
import com.supplygraph.application.connector.*;
import com.supplygraph.application.execution.*;
import com.supplygraph.application.port.out.*;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.security.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Observe -> Recommend -> Approve -> Execute -> Verify -> Reconcile -> Learn, wired through Spring against real PostgreSQL.
 * Every execution step in this test uses the SIMULATED sandbox adapter and says so; no external system is contacted.
 */
@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase10EndToEndIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("e2e10").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired DigitalTwinRepositoryPort twins;
    @Autowired RecoveryPlanRepositoryPort plans;
    @Autowired UserProfileRepositoryPort users;
    @Autowired RecoveryPlanConversionService conversion;
    @Autowired ExecutionApprovalService approvals;
    @Autowired ExecutionOrchestrationService orchestration;
    @Autowired ExecutionAcknowledgementService acks;
    @Autowired ExecutionReconciliationService reconciliation;
    @Autowired ExecutionCompensationService compensation;
    @Autowired ClosedLoopService closedLoop;
    @Autowired ExecutionQueryService queries;
    @Autowired ConnectorRegistryService registry;
    @Autowired ConnectorExecutionService connectorExecution;
    @Autowired OperationalEventIngestionService eventService;
    @Autowired SandboxExecutionAdapter sandbox;
    @Autowired DisruptionRepositoryPort disruptions;

    private DisruptionId newDisruption() {
        Instant now = Instant.now();
        Disruption d = Disruption.createDraft(WS, "EV_BATTERY", "SUPPLIER_OUTAGE", "Cell supplier outage", "e2e disruption", DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.HIGH,
                Set.of(NodeId.of("SUP-1")), Set.of(EdgeId.of("LANE-1")), Set.of(MaterialId.of("CELL-1")), now, now.plusSeconds(86400),
                new DisruptionImpactParameters(BigDecimal.valueOf(50), 86400L, 1, BigDecimal.valueOf(1000), "PIECES", Map.of()), "e2e");
        disruptions.save(d);
        return d.id();
    }

    static final WorkspaceId WS = WorkspaceId.of("ws-p10-e2e");
    static final WorkspaceId OTHER = WorkspaceId.of("ws-p10-other");
    static UserProfile manager, admin, sysadmin, logistics, analyst;
    static RecoveryPlan plan;
    static String workflowId;

    @AfterEach void clear() { SecurityContext.clear(); }

    private static void as(UserProfile u) { SecurityContext.setCurrentUser(u); }

    @BeforeEach void seed() {
        for (WorkspaceId w : List.of(WS, OTHER)) jdbc.update("INSERT INTO workspaces (id, name, industry, created_at, updated_at) VALUES (?, ?, 'EV_BATTERY', now(), now()) ON CONFLICT DO NOTHING", w.value(), w.value());
        if (twins.findByWorkspaceId(WS).isEmpty()) twins.save(new DigitalTwinState(WS, StateVersion.initial(), null, null));
        if (manager == null) {
            manager = users.save(UserProfile.create("e2e-mgr", "e2e-mgr", "m@x.io", "Mgr", UserRole.SUPPLY_CHAIN_MANAGER, Set.of(WS)));
            admin = users.save(UserProfile.create("e2e-admin", "e2e-admin", "a@x.io", "Admin", UserRole.WORKSPACE_ADMIN, Set.of(WS)));
            sysadmin = users.save(UserProfile.create("e2e-sys", "e2e-sys", "s@x.io", "Sys", UserRole.SYSTEM_ADMIN, Set.of(WS)));
            logistics = users.save(UserProfile.create("e2e-log", "e2e-log", "l@x.io", "Log", UserRole.LOGISTICS_MANAGER, Set.of(WS)));
            analyst = users.save(UserProfile.create("e2e-ana", "e2e-ana", "n@x.io", "Ana", UserRole.ANALYST, Set.of(WS)));
        }
    }

    @Test @Order(1) void observeViaSimulatedConnectorThenApprovedPlanConvertsToValidatedWorkflow() {
        as(manager);
        Connector c = registry.register(WS, "Sandbox TMS", ConnectorCategory.TMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox");
        c = registry.configure(c.id(), new ConnectorConfiguration(null, null, List.of(), 1000, 4096, 2));
        registry.activate(c.id());
        assertThat(connectorExecution.synchronize(c.id()).status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(eventService.getWorkspaceEvents(WS, 100)).anyMatch(e -> e.getEventType() == EventType.SHIPMENT_STATUS_CHANGED);   // [SIMULATED] observation

        StateVersion v = twins.findByWorkspaceId(WS).orElseThrow().getCurrentVersion();
        List<PlanAction> actions = List.of(
                new PlanAction("A1", PlanActionType.PURCHASE_ORDER_EXPEDITE, "Expedite PO 4711", null, null, new BigDecimal("100"), "PIECE", BigDecimal.TEN, 24, Map.of()),
                new PlanAction("A2", PlanActionType.INVENTORY_REALLOCATION, "Move buffer stock", null, null, new BigDecimal("50"), "PIECE", BigDecimal.ONE, 12, Map.of()));
        PlanComparisonMetrics metrics = new PlanComparisonMetrics(new BigDecimal("1000"), 10.0, 95.0, BigDecimal.ZERO, BigDecimal.ZERO, 60, 0.1, 0.9);
        RecoveryPlan p = RecoveryPlan.createDraft(newDisruption(), WS, v, null, "Recover supplier outage", "e2e", actions, metrics)
                .markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now()).approve(manager.id().value(), "approved by manager", v, Instant.now());
        plans.save(p);
        plan = p;

        ExecutionWorkflow w = conversion.convert(plan.id().value());
        assertThat(w.status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);
        assertThat(w.tasks()).hasSize(4);
        workflowId = w.id();
    }

    @Test @Order(2) void approvalGovernanceIsEnforcedServerSide() {
        as(manager);
        assertThatThrownBy(() -> approvals.approve(workflowId, WorkflowApproval.Scope.WORKFLOW, "self")).isInstanceOf(SeparationOfDutiesException.class);
        as(analyst);
        assertThatThrownBy(() -> approvals.approve(workflowId, WorkflowApproval.Scope.WORKFLOW, "x")).isInstanceOf(UnauthorizedOperationException.class);
        as(logistics);
        assertThatThrownBy(() -> orchestration.start(workflowId)).hasMessageContaining("READY");   // cannot execute before approval
        as(admin);
        assertThat(approvals.approve(workflowId, WorkflowApproval.Scope.WORKFLOW, "reviewed").status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);
        assertThatThrownBy(() -> approvals.approve(workflowId, WorkflowApproval.Scope.SENSITIVE_TASKS, "same person")).isInstanceOf(SeparationOfDutiesException.class);
        as(sysadmin);
        ExecutionWorkflow w = approvals.approve(workflowId, WorkflowApproval.Scope.SENSITIVE_TASKS, "second approver for the PO expedite");
        assertThat(w.status()).isEqualTo(WorkflowStatus.READY);
        assertThat(w.simulated()).isTrue();
    }

    @Test @Order(3) void executionRespectsDependenciesAndAcknowledgementsAreNotCompletion() {
        as(admin);
        assertThatThrownBy(() -> orchestration.start(workflowId)).isInstanceOf(SeparationOfDutiesException.class);   // approver cannot execute
        as(logistics);
        ExecutionWorkflow w = orchestration.start(workflowId);
        assertThat(w.status()).isEqualTo(WorkflowStatus.EXECUTING);
        assertThat(w.tasks().get(0).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(w.tasks().get(1).status()).isEqualTo(TaskStatus.PENDING);           // EXPEDITE waits for capacity confirmation to COMPLETE
        assertThat(sandbox.effectCount("task:" + w.tasks().get(1).id())).isZero();
        assertThat(queries.attempts(workflowId)).allSatisfy(a -> assertThat(a.simulated()).isTrue());
        assertThat(queries.evidence(workflowId)).allSatisfy(e -> assertThat(e.simulated()).isTrue());
        // concurrency: a second start must not run the workflow again
        assertThatThrownBy(() -> orchestration.start(workflowId)).isInstanceOf(com.supplygraph.domain.error.InvalidStateException.class);
    }

    @Test @Order(4) void acknowledgementsDriveCompletionIdempotentlyAndSimulatedEvidenceNeverTouchesTheTwin() {
        as(manager);
        int eventsBefore = eventService.getWorkspaceEvents(WS, 500).size();
        ExecutionWorkflow w = queries.get(workflowId);
        String t1 = w.tasks().get(0).id(), t2 = w.tasks().get(1).id(), t3 = w.tasks().get(2).id(), t4 = w.tasks().get(3).id();
        assertThat(acks.record(t1, "SIM-ACK-1", true, "sandbox-erp", "capacity confirmed")).isEqualTo(ExecutionAcknowledgementService.Result.APPLIED);
        assertThat(acks.record(t1, "SIM-ACK-1", true, "sandbox-erp", "capacity confirmed")).isEqualTo(ExecutionAcknowledgementService.Result.DUPLICATE);
        assertThat(queries.get(workflowId).task(t2).orElseThrow().status()).isEqualTo(TaskStatus.AWAITING_ACK);   // released
        acks.record(t2, "SIM-ACK-2", true, "sandbox-erp", null);
        acks.record(t3, "SIM-ACK-3", true, "sandbox-wms", null);
        acks.record(t4, "SIM-ACK-4", true, "sandbox-notify", null);
        w = queries.get(workflowId);
        assertThat(w.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(w.tasks()).allSatisfy(t -> assertThat(t.status()).isEqualTo(TaskStatus.COMPLETED));
        assertThat(eventService.getWorkspaceEvents(WS, 500)).hasSize(eventsBefore);                // simulated -> twin pipeline untouched
        assertThat(queries.timeline(workflowId)).allSatisfy(e -> assertThat(e.simulated()).isTrue());
    }

    @Test @Order(5) void closedLoopTreatsSimulatedActualsAsSimulatedAndUnknownAsUnknown() {
        as(manager);
        PlanOutcomeComparison before = closedLoop.comparison(workflowId);
        assertThat(before.rows()).allSatisfy(r -> assertThat(r.status()).isEqualTo(PlanOutcomeComparison.Status.UNKNOWN));
        closedLoop.recordActual(workflowId, OutcomeMetric.TOTAL_COST, 9000, "sandbox invoice", false);
        assertThat(closedLoop.comparison(workflowId).rows()).anyMatch(r -> r.metric() == OutcomeMetric.TOTAL_COST && r.status() == PlanOutcomeComparison.Status.SIMULATED);
        assertThat(closedLoop.evaluateDeviation(workflowId)).isEmpty();
        assertThat(closedLoop.requests(WS, 10)).isEmpty();
    }

    @Test @Order(6) void restartRecoveryAndReconciliationOfUnknownOutcomes() {
        // second workflow from a fresh plan, one task interrupted "mid-call" by a crash
        as(manager);
        StateVersion v = twins.findByWorkspaceId(WS).orElseThrow().getCurrentVersion();
        PlanAction a = new PlanAction("B1", PlanActionType.INVENTORY_REALLOCATION, "Move stock", null, null, new BigDecimal("20"), "PIECE", BigDecimal.ONE, 6, Map.of(SandboxExecutionAdapter.BEHAVIOR, "TIMEOUT"));
        RecoveryPlan p2 = RecoveryPlan.createDraft(newDisruption(), WS, v, null, "Plan 2", "e2e", List.of(a), null)
                .markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now()).approve(manager.id().value(), "ok", v, Instant.now());
        plans.save(p2);
        ExecutionWorkflow w = conversion.convert(p2.id().value());
        as(admin);
        w = approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "ok");
        assertThat(w.status()).isEqualTo(WorkflowStatus.READY);                     // no sensitive task -> single approval
        as(logistics);
        w = orchestration.start(w.id());
        ExecutionTask t1 = w.tasks().get(0);
        assertThat(t1.status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);      // sandbox TIMEOUT => unknown outcome
        assertThatThrownBy(() -> orchestration.retryTask(queries.get(p2WorkflowId(p2)).id(), t1.id())).hasMessageContaining("reconciled");
        assertThat(w.status()).isEqualTo(WorkflowStatus.RECONCILIATION_REQUIRED);

        as(manager);
        ExecutionWorkflow done = reconciliation.reconcile(t1.id(), ExecutionReconciliationService.Resolution.CONFIRMED_COMPLETED, "WMS shows allocation exists", Map.of("quantity", "20"));
        assertThat(done.tasks().get(0).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(queries.evidence(done.id())).anyMatch(e -> e.kind() == ExecutionEvidence.Kind.MANUAL_CONFIRMATION && e.simulated());
    }

    private String p2WorkflowId(RecoveryPlan p) { return queries.list(WS, 50).stream().filter(x -> x.planId().equals(p.id().value())).findFirst().orElseThrow().id(); }

    @Test @Order(7) void interruptedInProgressTasksAreRecoveredAsUnknownNotRetried() {
        as(manager);
        StateVersion v = twins.findByWorkspaceId(WS).orElseThrow().getCurrentVersion();
        PlanAction a = new PlanAction("C1", PlanActionType.PRODUCTION_RESCHEDULING, "Resequence", null, null, BigDecimal.ONE, "PIECE", BigDecimal.ONE, 6, Map.of());
        RecoveryPlan p3 = RecoveryPlan.createDraft(newDisruption(), WS, v, null, "Plan 3", "e2e", List.of(a), null)
                .markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now()).approve(manager.id().value(), "ok", v, Instant.now());
        plans.save(p3);
        ExecutionWorkflow w = conversion.convert(p3.id().value());
        as(admin);
        w = approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "ok");
        as(logistics);
        w = orchestration.start(w.id());
        // simulate crash: persist a task as IN_PROGRESS directly through the repository
        ExecutionWorkflowRepositoryPort repo = (ExecutionWorkflowRepositoryPort) org.springframework.test.util.AopTestUtils.getUltimateTargetObject(ctxRepo());
        ExecutionWorkflow raw = repo.findById(w.id()).orElseThrow();
        ExecutionTask stuck = raw.tasks().get(1);           // NOTIFY_STAKEHOLDERS, pending
        ExecutionTask ip = (stuck.status() == TaskStatus.PENDING ? stuck.markReady() : stuck).startAttempt();
        repo.save(raw.withTasks(raw.tasks().stream().map(t -> t.id().equals(ip.id()) ? ip : t).toList()));
        int attemptsBefore = queries.attempts(w.id()).size();
        assertThat(orchestration.recoverInterrupted()).isGreaterThanOrEqualTo(1);
        ExecutionWorkflow after = queries.get(w.id());
        assertThat(after.task(ip.id()).orElseThrow().status()).isEqualTo(TaskStatus.RECONCILIATION_REQUIRED);
        assertThat(queries.attempts(w.id())).hasSize(attemptsBefore);       // nothing was re-sent
        assertThat(queries.timeline(w.id())).anyMatch(e -> e.type().equals("RECOVERED_AFTER_RESTART"));
    }

    @Autowired ExecutionWorkflowRepositoryPort workflowRepo;
    private Object ctxRepo() { return workflowRepo; }

    @Test @Order(8) void workspaceIsolationAndDirectObjectReferencesAreRefused() {
        as(UserProfile.create("outsider", "outsider", "o@x.io", "O", UserRole.WORKSPACE_ADMIN, Set.of(OTHER)));
        assertThatThrownBy(() -> queries.get(workflowId)).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> queries.timeline(workflowId)).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> queries.list(WS, 10)).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> orchestration.cancel(workflowId, "idor")).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> acks.record(workflowId + "-T1", "R", true, "x", null)).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> conversion.convert(plan.id().value())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThat(queries.list(OTHER, 10)).isEmpty();
    }

    @Test @Order(9) void auditTrailRecordsConsequentialTransitions() {
        as(manager);
        // AuditGovernanceService requires AUDIT_VIEW; manager has it
        var trail = auditTrail();
        assertThat(trail).contains("WORKFLOW_CREATED", "WORKFLOW_APPROVED", "WORKFLOW_STARTED");
    }

    @Autowired com.supplygraph.application.audit.AuditGovernanceService audit;
    private List<String> auditTrail() { return audit.getAuditTrail(WS).stream().map(e -> e.action()).toList(); }

    @Test @Order(10) void concurrentStartOfTheSameWorkflowRunsItOnlyOnce() throws Exception {
        as(manager);
        StateVersion v = twins.findByWorkspaceId(WS).orElseThrow().getCurrentVersion();
        PlanAction a = new PlanAction("D1", PlanActionType.CAPACITY_REASSIGNMENT, "Reassign", null, null, BigDecimal.ONE, "PIECE", BigDecimal.ONE, 6, Map.of());
        RecoveryPlan p = RecoveryPlan.createDraft(newDisruption(), WS, v, null, "Plan 4", "e2e", List.of(a), null)
                .markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now()).approve(manager.id().value(), "ok", v, Instant.now());
        plans.save(p);
        ExecutionWorkflow w = conversion.convert(p.id().value());
        as(admin); approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "ok");
        as(sysadmin); w = approvals.approve(w.id(), WorkflowApproval.Scope.SENSITIVE_TASKS, "second");     // capacity reassignment is sensitive
        String id = w.id();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> run = () -> { SecurityContext.setCurrentUser(logistics); try { orchestration.start(id); return true; } catch (com.supplygraph.domain.error.InvalidStateException e) { return false; } finally { SecurityContext.clear(); } };
        List<Future<Boolean>> results = pool.invokeAll(List.of(run, run));
        pool.shutdown();
        long winners = results.stream().filter(f -> { try { return f.get(); } catch (Exception e) { return false; } }).count();
        assertThat(winners).isEqualTo(1);
        as(manager);
        assertThat(queries.attempts(id).stream().filter(x -> x.taskId().endsWith("-T1")).count()).isEqualTo(1);
    }
}
