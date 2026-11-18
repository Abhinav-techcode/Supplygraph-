package com.supplygraph.domain.model.execution;

/**
 * Deterministic guard against runaway replanning: threshold, duplicate suppression, bounded depth, and human decision requirement.
 * Termination: no material measured deviation; an open proposal already exists; or the depth limit is reached (escalate to humans).
 */
public final class ReassessmentGovernor {
    public enum Decision { NOT_NEEDED, PROPOSE, SUPPRESSED_DUPLICATE, LIMIT_REACHED }

    private ReassessmentGovernor() {}

    public static Decision decide(ExecutionWorkflow workflow, PlanOutcomeComparison comparison, ExecutionPolicy policy, boolean openProposalExists) {
        if (!comparison.isMaterial(policy.deviationThresholdPercent())) return Decision.NOT_NEEDED;
        if (openProposalExists) return Decision.SUPPRESSED_DUPLICATE;
        if (workflow.replanDepth() >= policy.maxReplanDepth()) return Decision.LIMIT_REACHED;
        return Decision.PROPOSE;
    }
}
