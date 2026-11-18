package com.supplygraph.application.execution;

import com.supplygraph.application.port.out.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import org.springframework.stereotype.Service;

import java.util.List;

/** Read model for the monitoring views. Every method checks workspace access against the workflow's own workspace. */
@Service
public class ExecutionQueryService {
    private final ExecutionSupport support;
    private final ExecutionWorkflowRepositoryPort workflows;
    private final ExecutionAttemptRepositoryPort attempts;
    private final ExecutionEvidenceRepositoryPort evidence;
    private final ExecutionEventRepositoryPort events;
    private final ExecutionPolicyRepositoryPort policies;

    public ExecutionQueryService(ExecutionSupport support, ExecutionWorkflowRepositoryPort workflows, ExecutionAttemptRepositoryPort attempts,
                                 ExecutionEvidenceRepositoryPort evidence, ExecutionEventRepositoryPort events, ExecutionPolicyRepositoryPort policies) {
        this.support = support; this.workflows = workflows; this.attempts = attempts; this.evidence = evidence; this.events = events; this.policies = policies;
    }

    public List<ExecutionWorkflow> list(WorkspaceId ws, int limit) { support.guard().enforceWorkspaceAccess(ws); return workflows.findByWorkspace(ws, limit); }
    public ExecutionWorkflow get(String id) { return support.load(id, null); }
    public List<ExecutionAttempt> attempts(String workflowId) { support.load(workflowId, null); return attempts.attemptsByWorkflow(workflowId); }
    public List<ExecutionEvidence> evidence(String workflowId) { support.load(workflowId, null); return evidence.evidenceByWorkflow(workflowId); }
    public List<ExecutionEvent> timeline(String workflowId) { support.load(workflowId, null); return events.eventsByWorkflow(workflowId); }
    public List<ExecutionEvent> workspaceTimeline(WorkspaceId ws, int limit) { support.guard().enforceWorkspaceAccess(ws); return events.eventsByWorkspace(ws, limit); }
    public ExecutionPolicy policy(WorkspaceId ws) { support.guard().enforceWorkspaceAccess(ws); return support.policy(ws); }

    public ExecutionPolicy savePolicy(ExecutionPolicy policy) {
        support.guard().enforceWorkspaceAndPermission(policy.workspaceId(), Permission.WORKSPACE_ADMIN);
        return policies.save(policy);
    }
}
