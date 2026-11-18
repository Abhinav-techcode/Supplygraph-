package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.ExecutionEvent;

import java.util.List;

public interface ExecutionEventRepositoryPort {
    ExecutionEvent append(ExecutionEvent event);
    List<ExecutionEvent> eventsByWorkflow(String workflowId);
    List<ExecutionEvent> eventsByWorkspace(WorkspaceId ws, int limit);
}
