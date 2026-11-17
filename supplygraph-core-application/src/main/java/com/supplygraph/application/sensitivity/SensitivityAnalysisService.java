package com.supplygraph.application.sensitivity;

import com.supplygraph.application.port.out.SensitivityAnalysisRepositoryPort;
import com.supplygraph.domain.model.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Systematically perturbs supply chain operational parameters to quantify recovery plan
 * elasticity, tail sensitivity, and overall robustness score.
 */
@Service
public class SensitivityAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SensitivityAnalysisService.class);

    private final SensitivityAnalysisRepositoryPort repository;

    public SensitivityAnalysisService(SensitivityAnalysisRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Executes parameter perturbation trials across lead time, disruption duration, demand surge,
     * buffer availability, and manufacturing yield rate.
     */
    public SensitivityAnalysisReport runSensitivitySuite(
            String workspaceId,
            String alternativeId,
            BigDecimal baselineCost,
            double baselineRecoveryDays,
            double baselineServiceLevelPct
    ) {
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(alternativeId, "alternativeId cannot be null");

        BigDecimal cost = baselineCost != null ? baselineCost : BigDecimal.valueOf(50000.00);
        double days = baselineRecoveryDays > 0 ? baselineRecoveryDays : 10.0;
        double service = baselineServiceLevelPct > 0 ? baselineServiceLevelPct : 95.0;

        String reportId = "SEN-RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<SensitivityTrialResult> trials = new ArrayList<>();

        double[] perturbations = {-0.25, -0.10, +0.10, +0.25, +0.50};

        for (SensitivityParameter param : SensitivityParameter.values()) {
            for (double delta : perturbations) {
                // Calculate impact factors depending on the parameter
                double costMultiplier = switch (param) {
                    case LEAD_TIME -> 1.0 + (delta * 0.45);
                    case DISRUPTION_DURATION -> 1.0 + (delta * 0.70);
                    case DEMAND_SURGE -> 1.0 + (delta * 0.55);
                    case INVENTORY_AVAILABILITY -> 1.0 - (delta * 0.30); // less inventory increases premium freight
                    case YIELD_RATE -> 1.0 - (delta * 0.40); // lower yield increases rework cost
                };

                double timeMultiplier = switch (param) {
                    case LEAD_TIME -> 1.0 + (delta * 0.80);
                    case DISRUPTION_DURATION -> 1.0 + (delta * 0.90);
                    case DEMAND_SURGE -> 1.0 + (delta * 0.25);
                    case INVENTORY_AVAILABILITY -> 1.0 - (delta * 0.40);
                    case YIELD_RATE -> 1.0 - (delta * 0.50);
                };

                BigDecimal trialCost = cost.multiply(BigDecimal.valueOf(Math.max(0.5, costMultiplier))).setScale(2, RoundingMode.HALF_UP);
                double trialDays = Math.max(1.0, days * timeMultiplier);
                double trialService = Math.min(100.0, Math.max(40.0, service - (Math.abs(delta) * (param == SensitivityParameter.DISRUPTION_DURATION ? 15.0 : 8.0))));

                // Elasticity: % change in cost / % change in input parameter
                double costPctChange = trialCost.subtract(cost).divide(cost, 4, RoundingMode.HALF_UP).doubleValue();
                double elasticity = delta != 0.0 ? Math.abs(costPctChange / delta) : 0.0;

                trials.add(SensitivityTrialResult.of(
                        SensitivityTrialId.generate(),
                        param,
                        delta * 100.0,
                        trialCost,
                        trialDays,
                        trialService,
                        Math.round(elasticity * 100.0) / 100.0
                ));
            }
        }

        // Robustness score: based on average elasticity (lower elasticity = higher robustness)
        double avgElasticity = trials.stream().mapToDouble(SensitivityTrialResult::elasticityIndex).average().orElse(1.0);
        double robustnessScore = Math.max(0.0, Math.min(1.0, 1.0 - (avgElasticity * 0.35)));

        SensitivityAnalysisReport report = SensitivityAnalysisReport.of(
                reportId,
                workspaceId,
                alternativeId,
                cost,
                days,
                service,
                trials,
                Math.round(robustnessScore * 100.0) / 100.0,
                Instant.now()
        );

        repository.save(report);
        log.info("Sensitivity report {} completed: {} trials evaluated, Robustness={}",
                reportId, trials.size(), report.overallRobustnessScore());
        return report;
    }

    public List<SensitivityAnalysisReport> findByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceId(workspaceId);
    }

    public Optional<SensitivityAnalysisReport> findById(String reportId) {
        return repository.findById(reportId);
    }

    public List<SensitivityAnalysisReport> findAll() {
        return repository.findAll();
    }
}
