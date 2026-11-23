package com.supplygraph.application.execution;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.*;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserProfile;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Shared plumbing for execution services: workspace-checked loading (IDOR safe), timeline events, audit, policy. */
@Component
public class ExecutionSupport {
    private final ExecutionWorkflowRepositoryPort workflows;
    private final ExecutionEventRepositoryPort events;
    private final ExecutionPolicyRepositoryPort policies;
    private final WorkspaceAccessGuard guard;
    private final AuditGovernanceService audit;

    public ExecutionSupport(ExecutionWorkflowRepositoryPort workflows, ExecutionEventRepositoryPort events, ExecutionPolicyRepositoryPort policies,
                            WorkspaceAccessGuard guard, AuditGovernanceService audit) {
        this.workflows = workflows; this.events = events; this.policies = policies; this.guard = guard; this.audit = audit;
    }

    /** Loads a workflow and enforces workspace access plus the permission against the workflow's own workspace. */
    public ExecutionWorkflow load(String id, Permission permission) {
        ExecutionWorkflow w = workflows.findById(id).orElseThrow(() -> new ResourceNotFoundException("Execution workflow not found: " + id));
        if (permission == null) guard.enforceWorkspaceAccess(w.workspaceId()); else guard.enforceWorkspaceAndPermission(w.workspaceId(), permission);
        return w;
    }

    public ExecutionWorkflow save(ExecutionWorkflow w) { return workflows.save(w); }

    /** Reloads without permission checks; callers must have authorized already. */
    public ExecutionWorkflow reload(String id) { return workflows.findById(id).orElseThrow(() -> new ResourceNotFoundException("Execution workflow not found: " + id)); }

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /** Serializes mutating operations per workflow inside this process (single-instance deployment); stale writes are additionally rejected by the repository. */
    public <T> T withLock(String workflowId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(workflowId, k -> new ReentrantLock());
        lock.lock();
        try { return action.get(); } finally { lock.unlock(); }
    }

    public static String workflowIdOf(String taskId) {
        int i = taskId.lastIndexOf("-T");
        if (i <= 0) throw new ValidationException("Malformed task id: " + taskId);
        return taskId.substring(0, i);
    }

    public ExecutionPolicy policy(WorkspaceId ws) { return policies.find(ws).orElseGet(() -> ExecutionPolicy.defaults(ws)); }

    public UserProfile user() {
        UserProfile u = SecurityContext.getCurrentUser();
        if (u == null) throw new com.supplygraph.domain.model.security.UnauthorizedOperationException("Authentication required: No active user session");
        return u;
    }

    public String actor() { UserProfile u = SecurityContext.getCurrentUser(); return u == null ? "SYSTEM" : u.id().value(); }

    public void timeline(ExecutionWorkflow w, String taskId, String type, String detail) {
        events.append(ExecutionEvent.of(w, taskId, type, actor(), detail));
    }

    public void audit(ExecutionWorkflow w, AuditAction action, String target, String previous, String next, String rationale) {
        UserProfile u = SecurityContext.getCurrentUser();
        audit.logAction(u == null ? null : u.id(), w.workspaceId(), action, target, fit(previous), fit(next), (w.simulated() ? "[SIMULATED] " : "") + rationale, null);
    }

    /** audit_logs state-version columns are 32 characters wide; never let an audit write fail on a long label. */
    private static String fit(String v) { return v == null ? null : v.length() <= 32 ? v : v.substring(0, 32); }

    public WorkspaceAccessGuard guard() { return guard; }

    /** Workflows holding at least one task stuck in {@code status} (used by restart recovery). */
    public java.util.List<ExecutionWorkflow> workflowsWithStuckTasks(TaskStatus status) { return workflows.findWithTaskStuck(status, java.time.Instant.now()); }
}
