package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Trade-off evaluation for a freight transportation lane comparing cost, speed, reliability, and carbon emissions.
 */
public record TransportModeTradeoff(
        TransportMode mode,
        BigDecimal freightCost,
        int transitDays,
        double co2KgPerTon,
        double reliabilityPercent
) implements Serializable {
    public TransportModeTradeoff {
        Objects.requireNonNull(mode, "Transport mode cannot be null");
        Objects.requireNonNull(freightCost, "Freight cost cannot be null");
    }
}

