package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.execution.ExecutionEvidence;

import java.util.List;

public interface ExecutionEvidenceRepositoryPort {
    ExecutionEvidence save(ExecutionEvidence evidence);
    List<ExecutionEvidence> evidenceByWorkflow(String workflowId);
    List<ExecutionEvidence> evidenceByTask(String taskId);
    boolean exists(String taskId, ExecutionEvidence.Kind kind, String reference);
}
