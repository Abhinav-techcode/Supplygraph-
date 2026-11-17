package com.supplygraph.domain.model.whatif;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Comparative differential evaluation between baseline digital twin state and What-If scenario.
 */
public record WhatIfComparisonResult(
        WhatIfScenarioId scenarioId,
        String scenarioName,
        BigDecimal baselineCostUsd,
        BigDecimal scenarioCostUsd,
        double baselineTimeToRecoverDays,
        double scenarioTimeToRecoverDays,
        double baselineServiceLevelPct,
        double scenarioServiceLevelPct,
        double baselineResilienceIndex,
        double scenarioResilienceIndex,
        double costVariancePct,
        double recoveryTimeDeltaDays,
        String varianceSummary,
        Instant evaluatedAt
) implements Serializable {

    public WhatIfComparisonResult {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(scenarioName, "scenarioName must not be null");
        baselineCostUsd = baselineCostUsd == null ? BigDecimal.ZERO : baselineCostUsd;
        scenarioCostUsd = scenarioCostUsd == null ? BigDecimal.ZERO : scenarioCostUsd;
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public static WhatIfComparisonResult of(
            WhatIfScenarioId scenarioId,
            String scenarioName,
            BigDecimal baselineCostUsd,
            BigDecimal scenarioCostUsd,
            double baselineTimeToRecoverDays,
            double scenarioTimeToRecoverDays,
            double baselineServiceLevelPct,
            double scenarioServiceLevelPct,
            double baselineResilienceIndex,
            double scenarioResilienceIndex,
            double costVariancePct,
            double recoveryTimeDeltaDays,
            String varianceSummary,
            Instant evaluatedAt
    ) {
        return new WhatIfComparisonResult(
                scenarioId, scenarioName, baselineCostUsd, scenarioCostUsd,
                baselineTimeToRecoverDays, scenarioTimeToRecoverDays,
                baselineServiceLevelPct, scenarioServiceLevelPct,
                baselineResilienceIndex, scenarioResilienceIndex,
                costVariancePct, recoveryTimeDeltaDays, varianceSummary, evaluatedAt
        );
    }
}
