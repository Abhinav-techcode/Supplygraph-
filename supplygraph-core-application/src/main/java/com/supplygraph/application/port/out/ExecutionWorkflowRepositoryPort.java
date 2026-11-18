package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.domain.model.execution.TaskStatus;
import com.supplygraph.domain.model.execution.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExecutionWorkflowRepositoryPort {
    /** Saves workflow, tasks and approvals atomically. Optimistic version check prevents lost updates. */
    ExecutionWorkflow save(ExecutionWorkflow workflow);
    Optional<ExecutionWorkflow> findById(String id);
    List<ExecutionWorkflow> findByWorkspace(WorkspaceId ws, int limit);
    List<ExecutionWorkflow> findByPlan(WorkspaceId ws, String planId);
    /** Workflows with at least one task currently in {@code status} whose update is older than {@code before}. */
    List<ExecutionWorkflow> findWithTaskStuck(TaskStatus status, Instant before);
    List<ExecutionWorkflow> findByStatus(WorkflowStatus status);
}
