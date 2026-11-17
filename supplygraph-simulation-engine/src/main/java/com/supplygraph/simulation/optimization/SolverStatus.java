package com.supplygraph.simulation.optimization;

/**
 * Mathematical solver status classifications.
 */
public enum SolverStatus {
    OPTIMAL,
    FEASIBLE,
    INFEASIBLE,
    UNKNOWN,
    TIME_LIMIT,
    ERROR;

    public boolean isFeasibleOrOptimal() {
        return this == OPTIMAL || this == FEASIBLE;
    }
}

