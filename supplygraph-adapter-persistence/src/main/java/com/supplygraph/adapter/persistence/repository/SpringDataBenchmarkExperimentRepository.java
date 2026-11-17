package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.BenchmarkExperimentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataBenchmarkExperimentRepository extends JpaRepository<BenchmarkExperimentJpaEntity, String> {
    List<BenchmarkExperimentJpaEntity> findByScenarioOrderByExecutedAtDesc(String scenario);
}

