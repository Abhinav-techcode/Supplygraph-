package com.supplygraph.simulation.montecarlo;

import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.simulation.engine.DiscreteEventSimulationEngine;
import com.supplygraph.simulation.engine.SimulationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Seeded, reproducible Monte Carlo analysis orchestrator.
 */
public class MonteCarloSimulator {
    private final DiscreteEventSimulationEngine simulationEngine = new DiscreteEventSimulationEngine();

    public MonteCarloSummaryResult run(
            DigitalTwinSnapshot snapshot,
            Disruption disruption,
            RecoveryPlan recoveryPlan,
            MonteCarloConfiguration config
    ) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(disruption, "disruption must not be null");
        Objects.requireNonNull(config, "config must not be null");

        String runId = "MC-" + UUID.randomUUID().toString().substring(0, 8);
        Random random = new Random(config.seed());
        int count = config.replicationCount();

        List<Double> delays = new ArrayList<>(count);
        List<BigDecimal> shortages = new ArrayList<>(count);
        List<BigDecimal> costs = new ArrayList<>(count);
        int slaViolations = 0;
        int recoveredWithinDeadlineCount = 0;

        double baseDelaySeconds = disruption.impactParameters().expectedDelaySeconds() != null
                ? disruption.impactParameters().expectedDelaySeconds()
                : 86400.0 * 3.0; // default 3 days

        for (int i = 0; i < count; i++) {
            // Sample perturbation parameter if configured, else default variation
            double sampledMultiplier = 1.0;
            if (!config.parameters().isEmpty()) {
                MonteCarloParameter param = config.parameters().get(0);
                sampledMultiplier = Math.max(0.2, param.distribution().sample(random));
            } else {
                // Default slight variance +/- 25%
                sampledMultiplier = 0.75 + random.nextDouble() * 0.5;
            }

            long simDelaySeconds = (long) (baseDelaySeconds * sampledMultiplier);
            double delayDays = simDelaySeconds / 86400.0;

            Disruption perturbedDisruption = new Disruption(
                    disruption.id(),
                    disruption.workspaceId(),
                    disruption.industryCode(),
                    disruption.scenarioType(),
                    disruption.title(),
                    disruption.description(),
                    disruption.category(),
                    disruption.severity(),
                    disruption.status(),
                    disruption.affectedNodeIds(),
                    disruption.affectedLaneIds(),
                    disruption.affectedMaterialIds(),
                    disruption.startTime(),
                    disruption.startTime().plusSeconds(simDelaySeconds),
                    new DisruptionImpactParameters(
                            disruption.impactParameters().capacityLossPercentage(),
                            simDelaySeconds,
                            (int) delayDays,
                            disruption.impactParameters().affectedQuantity(),
                            disruption.impactParameters().unitOfMeasure(),
                            disruption.impactParameters().customAttributes()
                    ),
                    disruption.sourceEvidence(),
                    disruption.createdAt(),
                    disruption.updatedAt(),
                    disruption.version()
            );

            SimulationResult simResult = simulationEngine.run(snapshot, perturbedDisruption, recoveryPlan, config.simulationConfig());

            delays.add(delayDays);
            shortages.add(simResult.totalShortageQuantity());

            BigDecimal simCost = simResult.totalShortageQuantity().multiply(BigDecimal.valueOf(125.0));
            costs.add(simCost);

            // SLA violation threshold: shortage > 0 or delay > 4 days
            boolean slaViolated = simResult.totalShortageQuantity().compareTo(BigDecimal.ZERO) > 0 || delayDays > 4.0;
            if (slaViolated) {
                slaViolations++;
            }

            // Recovery within deadline: recovered within 5 days
            if (delayDays <= 5.0) {
                recoveredWithinDeadlineCount++;
            }
        }

        // Calculate statistics
        Collections.sort(delays);
        double sumDelay = 0.0;
        for (double d : delays) {
            sumDelay += d;
        }
        double meanDelay = sumDelay / count;
        double medianDelay = delays.get(delays.size() / 2);
        int p90Idx = Math.min(delays.size() - 1, (int) Math.ceil(0.90 * delays.size()) - 1);
        int p95Idx = Math.min(delays.size() - 1, (int) Math.ceil(0.95 * delays.size()) - 1);
        double p90Delay = delays.get(p90Idx);
        double p95Delay = delays.get(p95Idx);

        BigDecimal sumShortage = BigDecimal.ZERO;
        BigDecimal maxShortage = BigDecimal.ZERO;
        for (BigDecimal s : shortages) {
            sumShortage = sumShortage.add(s);
            if (s.compareTo(maxShortage) > 0) {
                maxShortage = s;
            }
        }
        BigDecimal meanShortage = sumShortage.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        BigDecimal sumCost = BigDecimal.ZERO;
        for (BigDecimal c : costs) {
            sumCost = sumCost.add(c);
        }
        BigDecimal meanCost = sumCost.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        double slaViolationProb = (double) slaViolations / count;
        double recoveryProb = (double) recoveredWithinDeadlineCount / count;

        return new MonteCarloSummaryResult(
                runId,
                config.seed(),
                count,
                meanDelay,
                medianDelay,
                p90Delay,
                p95Delay,
                meanShortage,
                maxShortage,
                slaViolationProb,
                recoveryProb,
                meanCost,
                "COMPLETED",
                List.of()
        );
    }
}

