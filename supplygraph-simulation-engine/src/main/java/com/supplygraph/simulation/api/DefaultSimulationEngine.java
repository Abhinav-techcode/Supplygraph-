package com.supplygraph.simulation.api;

import com.supplygraph.simulation.optimization.OrToolsRecoveryOptimizer;

/**
 * Standard implementation of SimulationEngine API.
 */
public class DefaultSimulationEngine implements SimulationEngine {
    private final OrToolsRecoveryOptimizer optimizer = new OrToolsRecoveryOptimizer();

    @Override
    public String getVersion() {
        return "1.0.0-PHASE4";
    }

    @Override
    public boolean isSolverAvailable() {
        return optimizer.isNativeSolverAvailable();
    }
}

