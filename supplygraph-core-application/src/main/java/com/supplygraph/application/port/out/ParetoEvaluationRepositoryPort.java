package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.pareto.ParetoFrontier;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for multi-objective Pareto evaluations.
 */
public interface ParetoEvaluationRepositoryPort {

    void save(ParetoFrontier frontier);

    Optional<ParetoFrontier> findByEvaluationId(String evaluationId);

    Optional<ParetoFrontier> findLatestByDisruptionId(String disruptionId);

    List<ParetoFrontier> findAll();
}
