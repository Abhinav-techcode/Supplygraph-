package com.supplygraph.simulation.api;

/**
 * Public contract for discrete-event digital twin simulation.
 */
public interface SimulationEngine {
    String getVersion();
    boolean isSolverAvailable();
}

