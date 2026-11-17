package com.supplygraph.domain.model.quality;

import java.io.Serializable;
import java.util.Objects;

/**
 * An individual finding or violation identified during data quality rule evaluation.
 */
public record DataQualityFinding(
        String ruleId,
        QualityDimension dimension,
        String entityId,
        String field,
        String actualValue,
        String message,
        DataQualitySeverity severity,
        String suggestedRemediation
) implements Serializable {

    public DataQualityFinding {
        Objects.requireNonNull(ruleId, "Rule ID cannot be null");
        Objects.requireNonNull(dimension, "QualityDimension cannot be null");
        Objects.requireNonNull(entityId, "Entity ID cannot be null");
        Objects.requireNonNull(field, "Field name cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        if (suggestedRemediation == null) {
            suggestedRemediation = "Review and remediate the entity attributes in the master system";
        }
    }

    public static DataQualityFinding of(
            String ruleId,
            QualityDimension dimension,
            String entityId,
            String field,
            String actualValue,
            String message,
            DataQualitySeverity severity,
            String remediation
    ) {
        return new DataQualityFinding(ruleId, dimension, entityId, field, actualValue != null ? actualValue : "null", message, severity, remediation);
    }
}
