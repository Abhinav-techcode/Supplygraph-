package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.explainability.ExplainableRecommendation;
import com.supplygraph.domain.model.explainability.RecommendationId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for explainable recovery recommendations.
 */
public interface ExplainableRecommendationRepositoryPort {

    void save(ExplainableRecommendation recommendation);

    Optional<ExplainableRecommendation> findById(RecommendationId id);

    List<ExplainableRecommendation> findByDisruptionId(String disruptionId);

    List<ExplainableRecommendation> findAll();
}

