package com.supplygraph.domain.model.execution;

import java.time.Instant;
import java.util.Objects;

public record WorkflowApproval(String workflowId, String userId, String role, Scope scope, boolean approved, String rationale, Instant at) {
    public enum Scope { WORKFLOW, SENSITIVE_TASKS }
    public WorkflowApproval {
        Objects.requireNonNull(workflowId); Objects.requireNonNull(userId); Objects.requireNonNull(scope); Objects.requireNonNull(at);
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("rationale required");
    }
}
