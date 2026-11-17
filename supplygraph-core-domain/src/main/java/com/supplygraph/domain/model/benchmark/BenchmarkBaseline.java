package com.supplygraph.domain.model.benchmark;

/**
 * Four empirical coordination baselines evaluated under equivalent disruption conditions.
 */
public enum BenchmarkBaseline {
    UNCOORDINATED_AGENTS(
            "Baseline 1: Uncoordinated Agents",
            "Agents generate and submit proposals directly into recovery plans without inter-agent coordination or conflict arbitration."
    ),
    BASIC_RESOURCE_LOCKING(
            "Baseline 2: Basic Resource Locking",
            "First agent to claim a resource locks it exclusively. Subsequent agents attempting any claim on the same resource fail outright."
    ),
    CONFLICT_DETECTION_WITHOUT_SELECTIVE_REPLANNING(
            "Baseline 3: Conflict Detection (All-or-Nothing)",
            "ConcurGuard detects conflicts across proposals. If any conflict is detected, the entire candidate plan is aborted and all agents replan from scratch."
    ),
    CONCURGUARD_SELECTIVE_REPLANNING(
            "Baseline 4: ConcurGuard with Selective Replanning",
            "Full ConcurGuard arbitration detects conflicts, arbitrates claims, preserves unaffected valid actions, and isolates replanning scope to invalidated actions only."
    );

    private final String displayName;
    private final String description;

    BenchmarkBaseline(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

