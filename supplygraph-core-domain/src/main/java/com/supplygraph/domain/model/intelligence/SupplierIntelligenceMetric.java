package com.supplygraph.domain.model.intelligence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Deterministic quantitative indicator evaluated for a specific supplier with explicit provenance and confidence.
 */
public record SupplierIntelligenceMetric(
        String supplierId,
        SupplierMetricType metricType,
        double value,
        boolean measured,
        int sampleSize,
        String formulaDescription,
        double confidenceScore,
        Instant calculatedAt,
        String dataSource
) implements Serializable {

    public SupplierIntelligenceMetric {
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        Objects.requireNonNull(metricType, "SupplierMetricType cannot be null");
        calculatedAt = calculatedAt != null ? calculatedAt : Instant.now();
        dataSource = dataSource != null ? dataSource : "DIGITAL_TWIN_TELEMETRY";
        if (formulaDescription == null) {
            formulaDescription = metricType.description();
        }
    }

    public static SupplierIntelligenceMetric measured(
            String supplierId,
            SupplierMetricType metricType,
            double value,
            int sampleSize,
            String formula,
            String source
    ) {
        return new SupplierIntelligenceMetric(
                supplierId,
                metricType,
                value,
                true,
                sampleSize,
                formula,
                Math.min(1.0, 0.5 + (sampleSize * 0.05)),
                Instant.now(),
                source
        );
    }

    public static SupplierIntelligenceMetric estimated(
            String supplierId,
            SupplierMetricType metricType,
            double value,
            String formula,
            double confidence,
            String source
    ) {
        return new SupplierIntelligenceMetric(
                supplierId,
                metricType,
                value,
                false,
                0,
                formula,
                confidence,
                Instant.now(),
                source
        );
    }
}

