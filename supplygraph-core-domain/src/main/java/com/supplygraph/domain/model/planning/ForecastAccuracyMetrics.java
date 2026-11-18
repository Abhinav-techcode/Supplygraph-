package com.supplygraph.domain.model.planning;

import java.io.Serializable;

/**
 * Standard statistical error metrics evaluating forecast accuracy.
 */
public record ForecastAccuracyMetrics(
        double meanAbsoluteDeviation,
        double meanAbsolutePercentageError,
        double rootMeanSquareError,
        double trackingSignal
) implements Serializable {
    public static ForecastAccuracyMetrics zero() {
        return new ForecastAccuracyMetrics(0.0, 0.0, 0.0, 0.0);
    }
}

