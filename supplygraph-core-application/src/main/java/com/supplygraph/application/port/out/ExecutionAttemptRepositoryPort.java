package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.execution.ExecutionAttempt;

import java.util.List;

public interface ExecutionAttemptRepositoryPort {
    ExecutionAttempt save(ExecutionAttempt attempt);
    List<ExecutionAttempt> attemptsByWorkflow(String workflowId);
    List<ExecutionAttempt> attemptsByTask(String taskId);
}
