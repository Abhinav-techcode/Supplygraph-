package com.supplygraph.domain.model.execution;

import com.supplygraph.domain.model.WorkspaceId;

/** Per-workspace governance configuration (separation of duties, bounds, thresholds). Defaults are the safe choice. */
public record ExecutionPolicy(
        WorkspaceId workspaceId,
        boolean approverMustDifferFromCreator,
        boolean executorMustDifferFromApprover,
        boolean sensitiveTasksNeedSecondApproval,
        boolean allowLiveExecution,
        int maxTaskAttempts,
        int maxReplanDepth,
        double deviationThresholdPercent
) {
    public ExecutionPolicy {
        if (maxTaskAttempts < 1 || maxTaskAttempts > 10) throw new IllegalArgumentException("maxTaskAttempts must be 1..10");
        if (maxReplanDepth < 0 || maxReplanDepth > 5) throw new IllegalArgumentException("maxReplanDepth must be 0..5");
        if (deviationThresholdPercent <= 0 || deviationThresholdPercent > 100) throw new IllegalArgumentException("deviationThresholdPercent must be in (0,100]");
    }

    public static ExecutionPolicy defaults(WorkspaceId ws) { return new ExecutionPolicy(ws, true, true, true, false, 3, 2, 15.0); }
}
