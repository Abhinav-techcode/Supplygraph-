package com.supplygraph.domain.model.benchmark.statistical;

import com.supplygraph.domain.model.benchmark.BenchmarkBaseline;

import java.io.Serializable;
import java.util.Objects;

/**
 * Formal paired hypothesis testing result comparing coordination baselines.
 */
public record StatisticalSignificanceResult(
        BenchmarkBaseline baselineA,
        BenchmarkBaseline baselineB,
        String metricName,
        double meanA,
        double meanB,
        double meanDifference,
        double tStatistic,
        double pValue,
        boolean statisticallySignificant,
        double confidenceInterval95Lower,
        double confidenceInterval95Upper
) implements Serializable {

    public StatisticalSignificanceResult {
        Objects.requireNonNull(baselineA, "baselineA must not be null");
        Objects.requireNonNull(baselineB, "baselineB must not be null");
        Objects.requireNonNull(metricName, "metricName must not be null");
    }

    public static StatisticalSignificanceResult of(
            BenchmarkBaseline baselineA,
            BenchmarkBaseline baselineB,
            String metricName,
            double meanA,
            double meanB,
            double meanDifference,
            double tStatistic,
            double pValue,
            boolean statisticallySignificant,
            double confidenceInterval95Lower,
            double confidenceInterval95Upper
    ) {
        return new StatisticalSignificanceResult(
                baselineA, baselineB, metricName, meanA, meanB,
                meanDifference, tStatistic, pValue, statisticallySignificant,
                confidenceInterval95Lower, confidenceInterval95Upper
        );
    }
}
