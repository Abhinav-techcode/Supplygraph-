package com.supplygraph.simulation.montecarlo;

import java.math.BigDecimal;

/**
 * Result of a single stochastic replication.
 */
public record MonteCarloReplicationResult(
        int replicationIndex,
        long seedUsed,
        double delayDays,
        BigDecimal shortageQuantity,
        BigDecimal totalCost,
        boolean slaViolated,
        boolean recoveredWithinDeadline
) {
    public MonteCarloReplicationResult {
        shortageQuantity = shortageQuantity == null ? BigDecimal.ZERO : shortageQuantity;
        totalCost = totalCost == null ? BigDecimal.ZERO : totalCost;
    }
}

