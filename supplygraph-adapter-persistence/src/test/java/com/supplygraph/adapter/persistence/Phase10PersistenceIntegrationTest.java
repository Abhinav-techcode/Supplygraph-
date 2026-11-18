package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.PostgresExecutionPersistenceAdapter;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = Phase10PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 10 PostgreSQL execution persistence integration test")
class Phase10PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("p10").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired PostgresExecutionPersistenceAdapter repo;

    private ExecutionWorkflow workflow(String ws) {
        String id = "WF-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        ExecutionTask t1 = ExecutionTask.create(id, 1, TaskType.REQUEST_SUPPLIER_CAPACITY_CONFIRMATION, "confirm", Map.of("targetNodeId", "N&1=x", "quantity", "10"), List.of(), Map.of("quantity", "10"), 3);
        ExecutionTask t2 = ExecutionTask.create(id, 2, TaskType.EXPEDITE_PURCHASE_ORDER, "expedite", Map.of(), List.of(t1.id()), Map.of(), 3);
        return ExecutionWorkflow.draft(WorkspaceId.of(ws), "PLAN-1", "creator", "approver", "StateVersion[value=1]", List.of(t1, t2), "corr-1", 0);
    }

    @Test void workflowRoundTripPreservesTasksDependenciesApprovalsAndEncodedParameters() {
        ExecutionWorkflow w = workflow("ws-p10-a").validate().submitForApproval();
        w = w.addApproval(new WorkflowApproval(w.id(), "approver", "WORKSPACE_ADMIN", WorkflowApproval.Scope.WORKFLOW, true, "ok & fine", Instant.now()));
        repo.save(w);
        ExecutionWorkflow loaded = repo.findById(w.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);
        assertThat(loaded.tasks()).hasSize(2);
        assertThat(loaded.tasks().get(0).parameters()).containsEntry("targetNodeId", "N&1=x").containsEntry("quantity", "10");
        assertThat(loaded.tasks().get(1).dependsOn()).containsExactly(loaded.tasks().get(0).id());
        assertThat(loaded.approvals()).hasSize(1);
        assertThat(loaded.approvals().get(0).rationale()).isEqualTo("ok & fine");
        assertThat(loaded.version()).isEqualTo(w.version());
        loaded.graph().topologicalOrder();
    }

    @Test void updatesPersistTaskTransitionsAndApprovalsAreAppendOnly() {
        ExecutionWorkflow w = workflow("ws-p10-b").validate().submitForApproval();
        repo.save(w);
        w = w.addApproval(new WorkflowApproval(w.id(), "a1", "ADMIN", WorkflowApproval.Scope.WORKFLOW, true, "one", Instant.now()));
        repo.save(w);
        repo.save(w);   // idempotent re-save must not duplicate approvals
        ExecutionTask running = w.tasks().get(0).markReady().startAttempt().awaitAck("REF-1");
        w = w.withTasks(List.of(running, w.tasks().get(1)));
        repo.save(w);
        ExecutionWorkflow loaded = repo.findById(w.id()).orElseThrow();
        assertThat(loaded.approvals()).hasSize(1);
        assertThat(loaded.tasks().get(0).status()).isEqualTo(TaskStatus.AWAITING_ACK);
        assertThat(loaded.tasks().get(0).externalReference()).isEqualTo("REF-1");
        assertThat(loaded.tasks().get(0).attempts()).isEqualTo(1);
    }

    @Test void staleWritesAreRejected() {
        ExecutionWorkflow base = workflow("ws-p10-c");
        ExecutionWorkflow v1 = base.validate();
        ExecutionWorkflow v2 = v1.submitForApproval();
        repo.save(v2);
        assertThatThrownBy(() -> repo.save(v1)).isInstanceOf(ConcurrencyFailureException.class);
        assertThat(repo.findById(base.id()).orElseThrow().status()).isEqualTo(WorkflowStatus.PENDING_APPROVAL);
    }

    @Test void workspaceAndPlanQueriesAreIsolated() {
        ExecutionWorkflow a = workflow("ws-p10-d1"), b = workflow("ws-p10-d2");
        repo.save(a); repo.save(b);
        assertThat(repo.findByWorkspace(WorkspaceId.of("ws-p10-d1"), 50)).extracting(ExecutionWorkflow::id).containsExactly(a.id());
        assertThat(repo.findByPlan(WorkspaceId.of("ws-p10-d2"), "PLAN-1")).extracting(ExecutionWorkflow::id).containsExactly(b.id());
        assertThat(repo.findByWorkspace(WorkspaceId.of("nobody"), 50)).isEmpty();
    }

    @Test void interruptedTasksSurviveRestartAndAreFound() {
        ExecutionWorkflow w = workflow("ws-p10-e");
        ExecutionTask inProgress = w.tasks().get(0).markReady().startAttempt();
        repo.save(w.withTasks(List.of(inProgress, w.tasks().get(1))));
        assertThat(repo.findWithTaskStuck(TaskStatus.IN_PROGRESS, Instant.now().plusSeconds(1))).extracting(ExecutionWorkflow::id).contains(w.id());
        assertThat(repo.findWithTaskStuck(TaskStatus.FAILED, Instant.now().plusSeconds(1))).extracting(ExecutionWorkflow::id).doesNotContain(w.id());
    }

    @Test void attemptsEvidenceEventsAndOutcomesAreDurable() {
        ExecutionWorkflow w = workflow("ws-p10-f");
        repo.save(w);
        String taskId = w.tasks().get(0).id();
        ExecutionAttempt started = repo.save(ExecutionAttempt.start(w.id(), taskId, 1, "sandbox-execution", true, "task:" + taskId, "u1"));
        repo.save(started.finish(AttemptOutcome.UNKNOWN_OUTCOME, null, "timeout"));
        assertThat(repo.attemptsByTask(taskId)).hasSize(1).first().satisfies(a -> { assertThat(a.outcome()).isEqualTo(AttemptOutcome.UNKNOWN_OUTCOME); assertThat(a.simulated()).isTrue(); assertThat(a.finishedAt()).isNotNull(); });

        repo.save(ExecutionEvidence.of(w.id(), taskId, ExecutionEvidence.Kind.EXTERNAL_ACKNOWLEDGEMENT, true, "erp", "R1:OK", Map.of("q", "1"), Map.of("q", "1"), "ack", "u1"));
        assertThat(repo.exists(taskId, ExecutionEvidence.Kind.EXTERNAL_ACKNOWLEDGEMENT, "R1:OK")).isTrue();
        assertThat(repo.exists(taskId, ExecutionEvidence.Kind.EXTERNAL_ACKNOWLEDGEMENT, "R2:OK")).isFalse();
        assertThat(repo.evidenceByWorkflow(w.id())).hasSize(1).first().satisfies(e -> assertThat(e.observed()).containsEntry("q", "1"));

        repo.append(ExecutionEvent.of(w, taskId, "TASK_FAILED", "u1", "detail"));
        assertThat(repo.eventsByWorkflow(w.id())).hasSize(1);
        assertThat(repo.eventsByWorkspace(WorkspaceId.of("ws-p10-f"), 10)).hasSize(1);
        assertThat(repo.eventsByWorkspace(WorkspaceId.of("other"), 10)).isEmpty();

        repo.saveActual(new ActualMeasurement(w.id(), OutcomeMetric.TOTAL_COST, 1234.5, "ERP", false, "u1", Instant.now()));
        assertThat(repo.findActuals(w.id())).singleElement().satisfies(a -> assertThat(a.value()).isEqualTo(1234.5));

        ReassessmentRequest r = repo.saveReassessment(ReassessmentRequest.propose(w, 30, "cost", "SYSTEM"));
        repo.saveReassessment(r.decide(true, "u2", "go"));
        assertThat(repo.findReassessment(r.id()).orElseThrow().status()).isEqualTo(ReassessmentRequest.Status.APPROVED);
        assertThat(repo.findReassessmentsByWorkflow(w.id())).hasSize(1);
        assertThat(repo.findReassessmentsByWorkspace(WorkspaceId.of("ws-p10-f"), 10)).hasSize(1);
    }

    @Test void policyUpsertAndDefaults() {
        WorkspaceId ws = WorkspaceId.of("ws-p10-g");
        assertThat(repo.find(ws)).isEmpty();
        repo.save(ExecutionPolicy.defaults(ws));
        repo.save(new ExecutionPolicy(ws, false, true, true, true, 5, 1, 25.0));
        ExecutionPolicy p = repo.find(ws).orElseThrow();
        assertThat(p.approverMustDifferFromCreator()).isFalse();
        assertThat(p.allowLiveExecution()).isTrue();
        assertThat(p.maxTaskAttempts()).isEqualTo(5);
        assertThat(p.deviationThresholdPercent()).isEqualTo(25.0);
    }
}
