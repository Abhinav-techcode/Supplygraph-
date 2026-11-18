package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.application.port.out.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.*;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/** JDBC persistence for the Phase 10 execution aggregate and its append-only records. */
@Component
public class PostgresExecutionPersistenceAdapter implements ExecutionWorkflowRepositoryPort, ExecutionAttemptRepositoryPort, ExecutionEvidenceRepositoryPort,
        ExecutionEventRepositoryPort, ExecutionPolicyRepositoryPort, OutcomeRepositoryPort {

    private final JdbcTemplate jdbc;

    public PostgresExecutionPersistenceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // ---- encoding helpers
    static String enc(Map<String, String> m) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(m).forEach((k, v) -> { if (sb.length() > 0) sb.append('&'); sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8)); });
        return sb.toString();
    }
    static Map<String, String> dec(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String p : s.split("&")) { int i = p.indexOf('='); m.put(URLDecoder.decode(p.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8)); }
        return m;
    }
    private static Timestamp ts(Instant i) { return i == null ? null : Timestamp.from(i); }
    private static Instant inst(Timestamp t) { return t == null ? null : t.toInstant(); }

    // ---- workflows
    @Override @Transactional
    public ExecutionWorkflow save(ExecutionWorkflow w) {
        Long stored = jdbc.query("SELECT version FROM execution_workflows WHERE id = ?", rs -> rs.next() ? rs.getLong(1) : null, w.id());
        if (stored == null) {
            jdbc.update("INSERT INTO execution_workflows (id, workspace_id, plan_id, correlation_id, status, created_by, plan_approved_by, source_twin_version, simulated, replan_depth, status_reason, created_at, updated_at, version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    w.id(), w.workspaceId().value(), w.planId(), w.correlationId(), w.status().name(), w.createdBy(), w.planApprovedBy(), w.sourceTwinVersion(), w.simulated(), w.replanDepth(), w.statusReason(), ts(w.createdAt()), ts(w.updatedAt()), w.version());
        } else {
            if (w.version() < stored) throw new ConcurrencyFailureException("Stale write rejected for workflow " + w.id() + " (stored v" + stored + ", attempted v" + w.version() + ")");
            jdbc.update("UPDATE execution_workflows SET status=?, plan_approved_by=?, simulated=?, replan_depth=?, status_reason=?, updated_at=?, version=? WHERE id=?",
                    w.status().name(), w.planApprovedBy(), w.simulated(), w.replanDepth(), w.statusReason(), ts(w.updatedAt()), w.version(), w.id());
        }
        for (ExecutionTask t : w.tasks()) {
            int n = jdbc.update("UPDATE execution_tasks SET description=?, parameters=?, status=?, depends_on=?, condition_task_id=?, condition_outcome=?, attempts=?, max_attempts=?, expected_outcome=?, last_error=?, external_reference=?, simulated_evidence=?, updated_at=? WHERE id=?",
                    t.description(), enc(t.parameters()), t.status().name(), String.join(",", t.dependsOn()), t.conditionTaskId(), t.conditionOutcome() == null ? null : t.conditionOutcome().name(),
                    t.attempts(), t.maxAttempts(), enc(t.expectedOutcome()), t.lastError(), t.externalReference(), t.completedWithSimulatedEvidence(), ts(t.updatedAt()), t.id());
            if (n == 0) jdbc.update("INSERT INTO execution_tasks (id, workflow_id, sequence_no, task_type, description, parameters, status, depends_on, condition_task_id, condition_outcome, attempts, max_attempts, expected_outcome, last_error, external_reference, simulated_evidence, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    t.id(), t.workflowId(), t.sequence(), t.type().name(), t.description(), enc(t.parameters()), t.status().name(), String.join(",", t.dependsOn()), t.conditionTaskId(),
                    t.conditionOutcome() == null ? null : t.conditionOutcome().name(), t.attempts(), t.maxAttempts(), enc(t.expectedOutcome()), t.lastError(), t.externalReference(), t.completedWithSimulatedEvidence(), ts(t.updatedAt()));
        }
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM execution_approvals WHERE workflow_id = ?", Integer.class, w.id());
        List<WorkflowApproval> approvals = w.approvals();
        for (int i = existing == null ? 0 : existing; i < approvals.size(); i++) {   // approvals are append-only
            WorkflowApproval a = approvals.get(i);
            jdbc.update("INSERT INTO execution_approvals (workflow_id, user_id, role, scope, approved, rationale, decided_at) VALUES (?,?,?,?,?,?,?)",
                    w.id(), a.userId(), a.role(), a.scope().name(), a.approved(), a.rationale(), ts(a.at()));
        }
        return w;
    }

    @Override @Transactional(readOnly = true)
    public Optional<ExecutionWorkflow> findById(String id) {
        List<ExecutionWorkflow> l = jdbc.query("SELECT * FROM execution_workflows WHERE id = ?", (rs, i) -> hydrate(rs), id);
        return l.stream().findFirst();
    }

    @Override @Transactional(readOnly = true)
    public List<ExecutionWorkflow> findByWorkspace(WorkspaceId ws, int limit) {
        return jdbc.query("SELECT * FROM execution_workflows WHERE workspace_id = ? ORDER BY created_at DESC LIMIT ?", (rs, i) -> hydrate(rs), ws.value(), bound(limit));
    }

    @Override @Transactional(readOnly = true)
    public List<ExecutionWorkflow> findByPlan(WorkspaceId ws, String planId) {
        return jdbc.query("SELECT * FROM execution_workflows WHERE workspace_id = ? AND plan_id = ? ORDER BY created_at", (rs, i) -> hydrate(rs), ws.value(), planId);
    }

    @Override @Transactional(readOnly = true)
    public List<ExecutionWorkflow> findWithTaskStuck(TaskStatus status, Instant before) {
        List<String> ids = jdbc.queryForList("SELECT DISTINCT workflow_id FROM execution_tasks WHERE status = ? AND updated_at < ?", String.class, status.name(), ts(before));
        List<ExecutionWorkflow> out = new ArrayList<>();
        for (String id : ids) findById(id).ifPresent(out::add);
        return out;
    }

    @Override @Transactional(readOnly = true)
    public List<ExecutionWorkflow> findByStatus(WorkflowStatus status) {
        return jdbc.query("SELECT * FROM execution_workflows WHERE status = ? ORDER BY created_at", (rs, i) -> hydrate(rs), status.name());
    }

    private ExecutionWorkflow hydrate(java.sql.ResultSet rs) throws java.sql.SQLException {
        String id = rs.getString("id");
        List<ExecutionTask> tasks = jdbc.query("SELECT * FROM execution_tasks WHERE workflow_id = ? ORDER BY sequence_no", (r, i) -> new ExecutionTask(
                r.getString("id"), r.getString("workflow_id"), r.getInt("sequence_no"), TaskType.valueOf(r.getString("task_type")), r.getString("description"), dec(r.getString("parameters")),
                TaskStatus.valueOf(r.getString("status")), splitIds(r.getString("depends_on")), r.getString("condition_task_id"),
                r.getString("condition_outcome") == null ? null : TaskStatus.valueOf(r.getString("condition_outcome")), r.getInt("attempts"), r.getInt("max_attempts"),
                dec(r.getString("expected_outcome")), r.getString("last_error"), r.getString("external_reference"), r.getBoolean("simulated_evidence"), inst(r.getTimestamp("updated_at"))), id);
        List<WorkflowApproval> approvals = jdbc.query("SELECT * FROM execution_approvals WHERE workflow_id = ? ORDER BY id", (r, i) -> new WorkflowApproval(
                r.getString("workflow_id"), r.getString("user_id"), r.getString("role"), WorkflowApproval.Scope.valueOf(r.getString("scope")), r.getBoolean("approved"),
                r.getString("rationale"), inst(r.getTimestamp("decided_at"))), id);
        return new ExecutionWorkflow(id, WorkspaceId.of(rs.getString("workspace_id")), rs.getString("plan_id"), rs.getString("correlation_id"), WorkflowStatus.valueOf(rs.getString("status")),
                rs.getString("created_by"), rs.getString("plan_approved_by"), rs.getString("source_twin_version"), rs.getBoolean("simulated"), rs.getInt("replan_depth"), rs.getString("status_reason"),
                tasks, approvals, inst(rs.getTimestamp("created_at")), inst(rs.getTimestamp("updated_at")), rs.getLong("version"));
    }

    private static List<String> splitIds(String s) { return s == null || s.isBlank() ? List.of() : Arrays.asList(s.split(",")); }
    private static int bound(int l) { return Math.min(Math.max(l, 1), 1000); }

    // ---- attempts
    @Override @Transactional
    public ExecutionAttempt save(ExecutionAttempt a) {
        int n = jdbc.update("UPDATE execution_attempts SET outcome=?, external_reference=?, message=?, finished_at=? WHERE id=?", a.outcome().name(), a.externalReference(), a.message(), ts(a.finishedAt()), a.id());
        if (n == 0) jdbc.update("INSERT INTO execution_attempts (id, task_id, workflow_id, attempt_no, adapter_id, simulated, idempotency_key, outcome, external_reference, message, started_at, finished_at, requested_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                a.id(), a.taskId(), a.workflowId(), a.attemptNo(), a.adapterId(), a.simulated(), a.idempotencyKey(), a.outcome().name(), a.externalReference(), a.message(), ts(a.startedAt()), ts(a.finishedAt()), a.requestedBy());
        return a;
    }
    private static final RowMapper<ExecutionAttempt> ATTEMPT = (r, i) -> new ExecutionAttempt(r.getString("id"), r.getString("task_id"), r.getString("workflow_id"), r.getInt("attempt_no"),
            r.getString("adapter_id"), r.getBoolean("simulated"), r.getString("idempotency_key"), AttemptOutcome.valueOf(r.getString("outcome")), r.getString("external_reference"),
            r.getString("message"), inst(r.getTimestamp("started_at")), inst(r.getTimestamp("finished_at")), r.getString("requested_by"));
    @Override @Transactional(readOnly = true)
    public List<ExecutionAttempt> attemptsByWorkflow(String workflowId) { return jdbc.query("SELECT * FROM execution_attempts WHERE workflow_id = ? ORDER BY started_at, attempt_no", ATTEMPT, workflowId); }
    @Override @Transactional(readOnly = true)
    public List<ExecutionAttempt> attemptsByTask(String taskId) { return jdbc.query("SELECT * FROM execution_attempts WHERE task_id = ? ORDER BY attempt_no, started_at", ATTEMPT, taskId); }

    // ---- evidence
    @Override @Transactional
    public ExecutionEvidence save(ExecutionEvidence e) {
        jdbc.update("INSERT INTO execution_evidence (id, workflow_id, task_id, kind, simulated, source, reference, expected, observed, note, recorded_by, recorded_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                e.id(), e.workflowId(), e.taskId(), e.kind().name(), e.simulated(), e.source(), e.reference(), enc(e.expected()), enc(e.observed()), e.note(), e.recordedBy(), ts(e.recordedAt()));
        return e;
    }
    private static final RowMapper<ExecutionEvidence> EVIDENCE = (r, i) -> new ExecutionEvidence(r.getString("id"), r.getString("workflow_id"), r.getString("task_id"),
            ExecutionEvidence.Kind.valueOf(r.getString("kind")), r.getBoolean("simulated"), r.getString("source"), r.getString("reference"), dec(r.getString("expected")),
            dec(r.getString("observed")), r.getString("note"), r.getString("recorded_by"), inst(r.getTimestamp("recorded_at")));
    @Override @Transactional(readOnly = true)
    public List<ExecutionEvidence> evidenceByWorkflow(String id) { return jdbc.query("SELECT * FROM execution_evidence WHERE workflow_id = ? ORDER BY recorded_at", EVIDENCE, id); }
    @Override @Transactional(readOnly = true)
    public List<ExecutionEvidence> evidenceByTask(String id) { return jdbc.query("SELECT * FROM execution_evidence WHERE task_id = ? ORDER BY recorded_at", EVIDENCE, id); }
    @Override @Transactional(readOnly = true)
    public boolean exists(String taskId, ExecutionEvidence.Kind kind, String reference) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM execution_evidence WHERE task_id = ? AND kind = ? AND reference = ?", Integer.class, taskId, kind.name(), reference);
        return n != null && n > 0;
    }

    // ---- timeline
    @Override @Transactional
    public ExecutionEvent append(ExecutionEvent e) {
        jdbc.update("INSERT INTO execution_events (id, workflow_id, workspace_id, task_id, event_type, actor, detail, simulated, correlation_id, occurred_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                e.id(), e.workflowId(), e.workspaceId().value(), e.taskId(), e.type(), e.actor(), e.detail(), e.simulated(), e.correlationId(), ts(e.at()));
        return e;
    }
    private static final RowMapper<ExecutionEvent> EVENT = (r, i) -> new ExecutionEvent(r.getString("id"), r.getString("workflow_id"), WorkspaceId.of(r.getString("workspace_id")), r.getString("task_id"),
            r.getString("event_type"), r.getString("actor"), r.getString("detail"), r.getBoolean("simulated"), r.getString("correlation_id"), inst(r.getTimestamp("occurred_at")));
    @Override @Transactional(readOnly = true)
    public List<ExecutionEvent> eventsByWorkflow(String id) { return jdbc.query("SELECT * FROM execution_events WHERE workflow_id = ? ORDER BY occurred_at, id", EVENT, id); }
    @Override @Transactional(readOnly = true)
    public List<ExecutionEvent> eventsByWorkspace(WorkspaceId ws, int limit) { return jdbc.query("SELECT * FROM execution_events WHERE workspace_id = ? ORDER BY occurred_at DESC LIMIT ?", EVENT, ws.value(), bound(limit)); }

    // ---- policy
    @Override @Transactional(readOnly = true)
    public Optional<ExecutionPolicy> find(WorkspaceId ws) {
        return jdbc.query("SELECT * FROM execution_policies WHERE workspace_id = ?", (r, i) -> new ExecutionPolicy(ws, r.getBoolean("approver_differs_from_creator"), r.getBoolean("executor_differs_from_approver"),
                r.getBoolean("sensitive_second_approval"), r.getBoolean("allow_live_execution"), r.getInt("max_task_attempts"), r.getInt("max_replan_depth"), r.getDouble("deviation_threshold_percent")), ws.value()).stream().findFirst();
    }
    @Override @Transactional
    public ExecutionPolicy save(ExecutionPolicy p) {
        int n = jdbc.update("UPDATE execution_policies SET approver_differs_from_creator=?, executor_differs_from_approver=?, sensitive_second_approval=?, allow_live_execution=?, max_task_attempts=?, max_replan_depth=?, deviation_threshold_percent=? WHERE workspace_id=?",
                p.approverMustDifferFromCreator(), p.executorMustDifferFromApprover(), p.sensitiveTasksNeedSecondApproval(), p.allowLiveExecution(), p.maxTaskAttempts(), p.maxReplanDepth(), p.deviationThresholdPercent(), p.workspaceId().value());
        if (n == 0) jdbc.update("INSERT INTO execution_policies (workspace_id, approver_differs_from_creator, executor_differs_from_approver, sensitive_second_approval, allow_live_execution, max_task_attempts, max_replan_depth, deviation_threshold_percent) VALUES (?,?,?,?,?,?,?,?)",
                p.workspaceId().value(), p.approverMustDifferFromCreator(), p.executorMustDifferFromApprover(), p.sensitiveTasksNeedSecondApproval(), p.allowLiveExecution(), p.maxTaskAttempts(), p.maxReplanDepth(), p.deviationThresholdPercent());
        return p;
    }

    // ---- outcomes
    @Override @Transactional
    public ActualMeasurement saveActual(ActualMeasurement a) {
        jdbc.update("INSERT INTO plan_actuals (workflow_id, metric, metric_value, source, simulated, recorded_by, recorded_at) VALUES (?,?,?,?,?,?,?)",
                a.workflowId(), a.metric().name(), a.value(), a.source(), a.simulated(), a.recordedBy(), ts(a.recordedAt()));
        return a;
    }
    @Override @Transactional(readOnly = true)
    public List<ActualMeasurement> findActuals(String workflowId) {
        return jdbc.query("SELECT * FROM plan_actuals WHERE workflow_id = ? ORDER BY recorded_at, id", (r, i) -> new ActualMeasurement(r.getString("workflow_id"), OutcomeMetric.valueOf(r.getString("metric")),
                r.getDouble("metric_value"), r.getString("source"), r.getBoolean("simulated"), r.getString("recorded_by"), inst(r.getTimestamp("recorded_at"))), workflowId);
    }
    @Override @Transactional
    public ReassessmentRequest saveReassessment(ReassessmentRequest r) {
        int n = jdbc.update("UPDATE reassessment_requests SET status=?, decided_by=?, decision_rationale=?, decided_at=? WHERE id=?", r.status().name(), r.decidedBy(), r.decisionRationale(), ts(r.decidedAt()), r.id());
        if (n == 0) jdbc.update("INSERT INTO reassessment_requests (id, workflow_id, workspace_id, depth, deviation_percent, reason, status, proposed_by, decided_by, decision_rationale, created_at, decided_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                r.id(), r.workflowId(), r.workspaceId().value(), r.depth(), r.deviationPercent(), r.reason(), r.status().name(), r.proposedBy(), r.decidedBy(), r.decisionRationale(), ts(r.createdAt()), ts(r.decidedAt()));
        return r;
    }
    private static final RowMapper<ReassessmentRequest> REASSESS = (r, i) -> new ReassessmentRequest(r.getString("id"), r.getString("workflow_id"), WorkspaceId.of(r.getString("workspace_id")), r.getInt("depth"),
            r.getDouble("deviation_percent"), r.getString("reason"), ReassessmentRequest.Status.valueOf(r.getString("status")), r.getString("proposed_by"), r.getString("decided_by"),
            r.getString("decision_rationale"), inst(r.getTimestamp("created_at")), inst(r.getTimestamp("decided_at")));
    @Override @Transactional(readOnly = true)
    public Optional<ReassessmentRequest> findReassessment(String id) { return jdbc.query("SELECT * FROM reassessment_requests WHERE id = ?", REASSESS, id).stream().findFirst(); }
    @Override @Transactional(readOnly = true)
    public List<ReassessmentRequest> findReassessmentsByWorkflow(String id) { return jdbc.query("SELECT * FROM reassessment_requests WHERE workflow_id = ? ORDER BY created_at", REASSESS, id); }
    @Override @Transactional(readOnly = true)
    public List<ReassessmentRequest> findReassessmentsByWorkspace(WorkspaceId ws, int limit) { return jdbc.query("SELECT * FROM reassessment_requests WHERE workspace_id = ? ORDER BY created_at DESC LIMIT ?", REASSESS, ws.value(), bound(limit)); }
}
