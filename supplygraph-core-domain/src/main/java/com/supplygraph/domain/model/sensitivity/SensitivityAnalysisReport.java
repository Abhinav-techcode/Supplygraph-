package com.supplygraph.domain.model.sensitivity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated report summarizing sensitivity and robustness analysis across parameter trials.
 */
public record SensitivityAnalysisReport(
        String reportId,
        String workspaceId,
        String alternativeId,
        BigDecimal baselineCostUsd,
        double baselineRecoveryDays,
        double baselineServiceLevelPct,
        List<SensitivityTrialResult> trials,
        double overallRobustnessScore,
        Instant generatedAt
) implements Serializable {

    public SensitivityAnalysisReport {
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(alternativeId, "alternativeId must not be null");
        baselineCostUsd = baselineCostUsd == null ? BigDecimal.ZERO : baselineCostUsd;
        trials = trials == null ? List.of() : List.copyOf(trials);
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    }

    public static SensitivityAnalysisReport of(
            String reportId,
            String workspaceId,
            String alternativeId,
            BigDecimal baselineCostUsd,
            double baselineRecoveryDays,
            double baselineServiceLevelPct,
            List<SensitivityTrialResult> trials,
            double overallRobustnessScore,
            Instant generatedAt
    ) {
        return new SensitivityAnalysisReport(
                reportId, workspaceId, alternativeId,
                baselineCostUsd, baselineRecoveryDays, baselineServiceLevelPct,
                trials, overallRobustnessScore, generatedAt
        );
    }
}
