package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ParetoEvaluationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataParetoEvaluationRepository extends JpaRepository<ParetoEvaluationJpaEntity, String> {
    Optional<ParetoEvaluationJpaEntity> findTopByDisruptionIdOrderByEvaluatedAtDesc(String disruptionId);
}
