package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ExplainableRecommendationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataExplainableRecommendationRepository extends JpaRepository<ExplainableRecommendationJpaEntity, String> {
    List<ExplainableRecommendationJpaEntity> findByDisruptionId(String disruptionId);
}

