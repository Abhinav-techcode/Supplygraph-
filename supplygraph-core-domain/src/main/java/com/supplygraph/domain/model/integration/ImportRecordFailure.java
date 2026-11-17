package com.supplygraph.domain.model.integration;

import java.io.Serializable;
import java.util.Objects;

/**
 * Detailed error and correction metadata for an individual invalid import record.
 */
public record ImportRecordFailure(
        int rowIndex,
        String field,
        String rejectedValue,
        String errorCode,
        String message,
        String suggestedCorrection
) implements Serializable {

    public ImportRecordFailure {
        Objects.requireNonNull(field, "Field name cannot be null");
        Objects.requireNonNull(errorCode, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        if (suggestedCorrection == null) {
            suggestedCorrection = "Correct the input data format according to the specification";
        }
    }

    public static ImportRecordFailure of(int rowIndex, String field, String rejectedValue, String errorCode, String message, String suggestion) {
        return new ImportRecordFailure(rowIndex, field, rejectedValue != null ? rejectedValue : "null", errorCode, message, suggestion);
    }
}
