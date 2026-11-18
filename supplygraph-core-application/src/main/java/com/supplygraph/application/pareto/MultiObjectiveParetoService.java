package com.supplygraph.application.pareto;

import com.supplygraph.application.port.out.ParetoEvaluationRepositoryPort;
import com.supplygraph.domain.model.pareto.ParetoFrontier;
import com.supplygraph.domain.model.pareto.ParetoSolution;
import com.supplygraph.domain.model.pareto.ParetoSolutionId;
import com.supplygraph.domain.model.recovery.RecoveryPlanAlternative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Multi-objective decision intelligence evaluating recovery alternatives across
 * 5 conflicting dimensions (cost, time, unmet demand, service degradation, risk)
 * to compute the Pareto-optimal frontier.
 */
@Service
public class MultiObjectiveParetoService {

    private static final Logger log = LoggerFactory.getLogger(MultiObjectiveParetoService.class);

    private final ParetoEvaluationRepositoryPort repository;

    public MultiObjectiveParetoService(ParetoEvaluationRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Evaluates a collection of candidate recovery alternatives and returns the non-dominated Pareto frontier.
     */
    public ParetoFrontier evaluateAlternatives(String disruptionId, List<RecoveryPlanAlternative> alternatives) {
        Objects.requireNonNull(disruptionId, "disruptionId cannot be null");

        String evaluationId = "PAR-EVAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<ParetoSolution> candidates = new ArrayList<>();

        if (alternatives != null && !alternatives.isEmpty()) {
            for (RecoveryPlanAlternative alt : alternatives) {
                // Map alternative to multi-objective dimensions
                BigDecimal cost = alt.expectedCost() != null ? alt.expectedCost() : BigDecimal.valueOf(50000);
                double recoveryTime = alt.timeToRecoverHours();
                // Derived objectives
                double unmetDemand = Math.max(0.0, (100.0 - alt.serviceLevelRestoration()) * 25.0);
                double serviceDegradation = Math.max(0.0, 100.0 - alt.serviceLevelRestoration());
                double concentrationRisk = 0.25; // default moderate concentration

                String explanation = String.format("Cost $%s, %.1f hrs, %.1f%% service",
                        cost, recoveryTime, alt.serviceLevelRestoration());

                candidates.add(ParetoSolution.of(
                        ParetoSolutionId.generate(),
                        alt.id().value(),
                        alt.name(),
                        cost,
                        recoveryTime,
                        unmetDemand,
                        serviceDegradation,
                        concentrationRisk,
                        false, // computed by ParetoFrontier.compute()
                        0.95,
                        explanation
                ));
            }
        } else {
            // Default sample candidate set if no alternatives passed
            candidates.add(ParetoSolution.of(
                    ParetoSolutionId.generate(),
                    "ALT-EXPEDITE",
                    "Emergency Air Freight Expedited",
                    BigDecimal.valueOf(85000.00),
                    36.0,
                    150.0,
                    2.0,
                    0.20,
                    false,
                    0.98,
                    "High cost premium, fastest recovery time, best service retention"
            ));
            candidates.add(ParetoSolution.of(
                    ParetoSolutionId.generate(),
                    "ALT-BUFFER",
                    "Regional Buffer Stock Depletion",
                    BigDecimal.valueOf(42000.00),
                    72.0,
                    450.0,
                    6.0,
                    0.45,
                    false,
                    0.92,
                    "Balanced cost and duration, moderate inventory risk"
            ));
            candidates.add(ParetoSolution.of(
                    ParetoSolutionId.generate(),
                    "ALT-SLOW",
                    "Standard Reorder via Rail/Ocean",
                    BigDecimal.valueOf(18000.00),
                    180.0,
                    1200.0,
                    18.0,
                    0.10,
                    false,
                    0.99,
                    "Lowest direct expenditure, prolonged outage and severe customer delay"
            ));
        }

        ParetoFrontier frontier = ParetoFrontier.compute(evaluationId, disruptionId, candidates);
        repository.save(frontier);
        log.info("Evaluated {} alternatives for disruption {}. Found {} Pareto-optimal solutions.",
                candidates.size(), disruptionId, frontier.frontierSolutions().size());
        return frontier;
    }

    public Optional<ParetoFrontier> findLatestByDisruptionId(String disruptionId) {
        return repository.findLatestByDisruptionId(disruptionId);
    }

    public List<ParetoFrontier> findAll() {
        return repository.findAll();
    }
}

