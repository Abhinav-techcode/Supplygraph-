package com.supplygraph.domain.model.execution;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.UUID;

/** Append-only timeline/provenance entry for a workflow. */
public record ExecutionEvent(String id, String workflowId, WorkspaceId workspaceId, String taskId, String type, String actor, String detail,
                             boolean simulated, String correlationId, Instant at) {
    public static ExecutionEvent of(ExecutionWorkflow w, String taskId, String type, String actor, String detail) {
        return new ExecutionEvent(UUID.randomUUID().toString(), w.id(), w.workspaceId(), taskId, type, actor, detail, w.simulated(), w.correlationId(), Instant.now());
    }
}
