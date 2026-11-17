package com.supplygraph.domain.model.whatif;

import java.io.Serializable;
import java.util.Objects;

/**
 * Parameter configuration for an isolated What-If scenario evaluation.
 * Evaluates alternative futures without mutating the active production digital twin.
 */
public record WhatIfScenarioConfig(
        WhatIfScenarioId scenarioId,
        String scenarioName,
        String description,
        String workspaceId,
        double supplierCapacityFactor,
        double safetyStockMultiplier,
        int leadTimeDeltaDays,
        String freightModeOverride,
        int disruptionDurationDaysOverride,
        double costWeight,
        double serviceWeight,
        double riskWeight
) implements Serializable {

    public WhatIfScenarioConfig {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(scenarioName, "scenarioName must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
    }

    public static WhatIfScenarioConfig of(
            WhatIfScenarioId scenarioId,
            String scenarioName,
            String description,
            String workspaceId,
            double supplierCapacityFactor,
            double safetyStockMultiplier,
            int leadTimeDeltaDays,
            String freightModeOverride,
            int disruptionDurationDaysOverride,
            double costWeight,
            double serviceWeight,
            double riskWeight
    ) {
        return new WhatIfScenarioConfig(
                scenarioId, scenarioName, description, workspaceId,
                supplierCapacityFactor, safetyStockMultiplier, leadTimeDeltaDays,
                freightModeOverride, disruptionDurationDaysOverride,
                costWeight, serviceWeight, riskWeight
        );
    }
}
