package com.supplygraph.domain.model.quality;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for data quality assessment reports.
 */
public record DataQualityReportId(String value) implements Serializable {

    public DataQualityReportId {
        Objects.requireNonNull(value, "Data quality report ID value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Data quality report ID cannot be blank");
        }
    }

    public static DataQualityReportId generate() {
        return new DataQualityReportId("DQR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public static DataQualityReportId of(String value) {
        return new DataQualityReportId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

