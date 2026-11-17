package com.supplygraph.application.whatif;

import com.supplygraph.application.port.out.WhatIfExperimentRepositoryPort;
import com.supplygraph.domain.model.whatif.WhatIfComparisonResult;
import com.supplygraph.domain.model.whatif.WhatIfScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Isolated What-If scenario execution engine. Simulates counterfactual parameters and
 * alternative constraints without modifying the production digital twin state.
 */
@Service
public class WhatIfAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(WhatIfAnalysisService.class);

    private final WhatIfExperimentRepositoryPort repository;

    public WhatIfAnalysisService(WhatIfExperimentRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Executes isolated What-If scenario calculation against baseline metrics.
     */
    public WhatIfComparisonResult executeScenario(WhatIfScenarioConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        // Baseline production metrics
        BigDecimal baselineCost = BigDecimal.valueOf(50000.00);
        double baselineTTR = 12.0; // days
        double baselineService = 92.0; // %
        double baselineResilience = 65.0; // /100

        // Parameter adjustments derived from What-If configuration
        // 1. Supplier capacity factor (e.g. 0.8 reduces available volume, driving premium expediting)
        double capacityPenaltyFactor = config.supplierCapacityFactor() < 1.0 ? (1.0 - config.supplierCapacityFactor()) * 0.4 : 0.0;

        // 2. Safety stock multiplier (e.g. 1.5 increases holding cost but reduces stockout delay)
        double stockBufferFactor = config.safetyStockMultiplier() >= 1.0 ? (config.safetyStockMultiplier() - 1.0) * 0.15 : -0.2;

        // 3. Freight mode and lead time delta
        double freightModeCostFactor = "AIR".equalsIgnoreCase(config.freightModeOverride()) ? 0.35 :
                "SEA".equalsIgnoreCase(config.freightModeOverride()) ? -0.15 : 0.0;

        double freightModeTimeFactor = "AIR".equalsIgnoreCase(config.freightModeOverride()) ? -4.0 :
                "SEA".equalsIgnoreCase(config.freightModeOverride()) ? 5.0 : 0.0;

        // Compute scenario outcomes
        BigDecimal scenarioCost = baselineCost
                .multiply(BigDecimal.valueOf(1.0 + capacityPenaltyFactor + stockBufferFactor + freightModeCostFactor))
                .setScale(2, RoundingMode.HALF_UP);

        double scenarioTTR = Math.max(1.0, baselineTTR + config.leadTimeDeltaDays() + freightModeTimeFactor - (stockBufferFactor * 10));
        double scenarioService = Math.min(100.0, Math.max(50.0, baselineService + (config.safetyStockMultiplier() > 1.0 ? 5.0 : -8.0) - (capacityPenaltyFactor * 20)));
        double scenarioResilience = Math.min(100.0, Math.max(10.0, baselineResilience + (config.safetyStockMultiplier() > 1.0 ? 12.0 : -15.0) - (config.disruptionDurationDaysOverride() > 7 ? 8.0 : 0.0)));

        double costVariancePct = scenarioCost.subtract(baselineCost)
                .divide(baselineCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        double recoveryTimeDelta = scenarioTTR - baselineTTR;

        String varianceSummary = String.format(
                "Scenario [%s] yields %+.1f%% cost variance ($%s vs $%s) and %+.1f days TTR (%s vs %s days). Service level %s from %.1f%% to %.1f%%.",
                config.scenarioName(),
                costVariancePct,
                scenarioCost,
                baselineCost,
                recoveryTimeDelta,
                scenarioTTR,
                baselineTTR,
                scenarioService >= baselineService ? "improves" : "degrades",
                baselineService,
                scenarioService
        );

        WhatIfComparisonResult result = WhatIfComparisonResult.of(
                config.scenarioId(),
                config.scenarioName(),
                baselineCost,
                scenarioCost,
                baselineTTR,
                scenarioTTR,
                baselineService,
                scenarioService,
                baselineResilience,
                scenarioResilience,
                costVariancePct,
                recoveryTimeDelta,
                varianceSummary,
                Instant.now()
        );

        repository.save(result);
        log.info("What-If scenario {} executed: Cost delta={}%, TTR delta={} days",
                config.scenarioName(), costVariancePct, recoveryTimeDelta);
        return result;
    }

    public List<WhatIfComparisonResult> findAll() {
        return repository.findAll();
    }

    public Optional<WhatIfComparisonResult> findByScenarioId(com.supplygraph.domain.model.whatif.WhatIfScenarioId scenarioId) {
        return repository.findByScenarioId(scenarioId);
    }
}
