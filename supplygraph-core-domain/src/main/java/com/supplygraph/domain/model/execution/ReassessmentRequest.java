package com.supplygraph.domain.model.execution;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.UUID;

/** Governed proposal to reassess a plan after material deviation. Never acts on its own: a human must decide. */
public record ReassessmentRequest(String id, String workflowId, WorkspaceId workspaceId, int depth, double deviationPercent, String reason,
                                  Status status, String proposedBy, String decidedBy, String decisionRationale, Instant createdAt, Instant decidedAt) {
    public enum Status { PROPOSED, APPROVED, DECLINED }

    public static ReassessmentRequest propose(ExecutionWorkflow w, double deviationPercent, String reason, String proposedBy) {
        return new ReassessmentRequest(UUID.randomUUID().toString(), w.id(), w.workspaceId(), w.replanDepth() + 1, deviationPercent, reason,
                Status.PROPOSED, proposedBy, null, null, Instant.now(), null);
    }

    public ReassessmentRequest decide(boolean approve, String by, String rationale) {
        if (status != Status.PROPOSED) throw new IllegalStateException("Request already " + status);
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("rationale required");
        return new ReassessmentRequest(id, workflowId, workspaceId, depth, deviationPercent, reason, approve ? Status.APPROVED : Status.DECLINED, proposedBy, by, rationale, createdAt, Instant.now());
    }
}
