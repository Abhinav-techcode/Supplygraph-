package com.supplygraph.domain.model.quality;

/**
 * Severity impact classification for data quality anomalies.
 */
public enum DataQualitySeverity {
    CRITICAL(1.0, "Critical issue compromising planning integrity or simulation execution"),
    WARNING(0.5, "Suboptimal data quality that degrades optimization precision"),
    INFO(0.1, "Informational advisory or minor formatting anomaly");

    private final double penaltyWeight;
    private final String description;

    DataQualitySeverity(double penaltyWeight, String description) {
        this.penaltyWeight = penaltyWeight;
        this.description = description;
    }

    public double penaltyWeight() {
        return penaltyWeight;
    }

    public String description() {
        return description;
    }
}

