package com.supplygraph.domain.model.sensitivity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Result of perturbing an operational parameter by a designated percentage delta.
 */
public record SensitivityTrialResult(
        SensitivityTrialId trialId,
        SensitivityParameter parameter,
        double perturbationPercentage,
        BigDecimal resultantCostUsd,
        double resultantRecoveryDays,
        double resultantServiceLevelPct,
        double elasticityIndex
) implements Serializable {

    public SensitivityTrialResult {
        Objects.requireNonNull(trialId, "trialId must not be null");
        Objects.requireNonNull(parameter, "parameter must not be null");
        resultantCostUsd = resultantCostUsd == null ? BigDecimal.ZERO : resultantCostUsd;
    }

    public static SensitivityTrialResult of(
            SensitivityTrialId trialId,
            SensitivityParameter parameter,
            double perturbationPercentage,
            BigDecimal resultantCostUsd,
            double resultantRecoveryDays,
            double resultantServiceLevelPct,
            double elasticityIndex
    ) {
        return new SensitivityTrialResult(
                trialId, parameter, perturbationPercentage,
                resultantCostUsd, resultantRecoveryDays, resultantServiceLevelPct,
                elasticityIndex
        );
    }
}

