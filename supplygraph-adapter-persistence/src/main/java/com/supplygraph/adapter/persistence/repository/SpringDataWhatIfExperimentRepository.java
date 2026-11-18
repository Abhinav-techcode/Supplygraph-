package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.WhatIfExperimentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataWhatIfExperimentRepository extends JpaRepository<WhatIfExperimentJpaEntity, String> {
    Optional<WhatIfExperimentJpaEntity> findTopByScenarioIdOrderByEvaluatedAtDesc(String scenarioId);
    List<WhatIfExperimentJpaEntity> findByScenarioIdOrderByEvaluatedAtDesc(String scenarioId);
}

