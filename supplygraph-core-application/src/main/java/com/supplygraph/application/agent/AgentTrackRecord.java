package com.supplygraph.application.agent;

/** How many of an agent's incident proposals people accepted. Plain counts only: no score is estimated before there are decisions to count. */
public record AgentTrackRecord(String role, int trials, int successes) {
    public static AgentTrackRecord empty(String role) { return new AgentTrackRecord(role, 0, 0); }

    /** Text for screens: the raw counts, or an explicit statement that nothing has been decided yet. */
    public String summary() {
        return trials == 0 ? "No decisions yet" : "Accepted " + successes + " of " + trials + " decisions";
    }
}
