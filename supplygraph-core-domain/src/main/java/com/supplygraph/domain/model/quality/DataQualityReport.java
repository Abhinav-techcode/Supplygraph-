package com.supplygraph.domain.model.quality;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable assessment summarizing data quality compliance and anomalies across a digital twin workspace.
 */
public record DataQualityReport(
        DataQualityReportId id,
        String workspaceId,
        Instant evaluatedAt,
        double overallScore,
        Map<QualityDimension, Double> dimensionScores,
        List<DataQualityFinding> findings,
        int totalEntitiesEvaluated,
        int cleanRecordsCount,
        int flawedRecordsCount
) implements Serializable {

    public DataQualityReport {
        Objects.requireNonNull(id, "DataQualityReportId cannot be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
        dimensionScores = dimensionScores != null ? Collections.unmodifiableMap(dimensionScores) : Collections.emptyMap();
        findings = findings != null ? Collections.unmodifiableList(findings) : Collections.emptyList();
    }

    public static DataQualityReport of(
            String workspaceId,
            double overallScore,
            Map<QualityDimension, Double> dimensionScores,
            List<DataQualityFinding> findings,
            int totalEntities,
            int cleanCount,
            int flawedCount
    ) {
        return new DataQualityReport(
                DataQualityReportId.generate(),
                workspaceId,
                Instant.now(),
                Math.max(0.0, Math.min(100.0, overallScore)),
                dimensionScores,
                findings,
                totalEntities,
                cleanCount,
                flawedCount
        );
    }

    public int criticalFindingCount() {
        return (int) findings.stream().filter(f -> f.severity() == DataQualitySeverity.CRITICAL).count();
    }

    public int warningFindingCount() {
        return (int) findings.stream().filter(f -> f.severity() == DataQualitySeverity.WARNING).count();
    }
}
