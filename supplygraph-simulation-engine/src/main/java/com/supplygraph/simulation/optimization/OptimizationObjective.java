package com.supplygraph.simulation.optimization;

/**
 * Supported objective functions for recovery optimization.
 */
public enum OptimizationObjective {
    MINIMIZE_COST,
    MINIMIZE_DELAY,
    MINIMIZE_UNMET_DEMAND,
    WEIGHTED_MULTI_OBJECTIVE
}

