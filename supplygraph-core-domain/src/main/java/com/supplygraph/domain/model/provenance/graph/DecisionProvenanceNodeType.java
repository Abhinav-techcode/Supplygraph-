package com.supplygraph.domain.model.provenance.graph;

/**
 * Node categories in the end-to-end decision provenance graph.
 */
public enum DecisionProvenanceNodeType {
    DISRUPTION("Disruption Event"),
    EVIDENCE("Telemetry & Evidence"),
    TWIN_SNAPSHOT("Digital Twin State Snapshot"),
    IMPACT_ASSESSMENT("Cascading Impact Assessment"),
    ALERT("Operational Alert"),
    INVESTIGATION("Agent Investigation"),
    PROPOSAL("Recovery Proposal"),
    CONCURGUARD_DECISION("ConcurGuard Arbitration"),
    SIMULATION_RUN("Discrete-Event Simulation Run"),
    MONTE_CARLO_RUN("Monte Carlo Risk Analysis"),
    OPTIMIZATION_RUN("OR-Tools Optimization"),
    RECOVERY_ALTERNATIVE("Recovery Alternative"),
    HUMAN_DECISION("Human Decision Approval/Rejection");

    private final String displayName;

    DecisionProvenanceNodeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
