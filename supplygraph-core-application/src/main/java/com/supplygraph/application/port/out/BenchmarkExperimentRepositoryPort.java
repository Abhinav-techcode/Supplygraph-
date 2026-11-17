package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.benchmark.BenchmarkExperimentRecord;
import com.supplygraph.domain.model.benchmark.BenchmarkScenarioType;
import com.supplygraph.domain.model.benchmark.ExperimentId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for persisting and retrieving reproducible research benchmark experiment trials.
 */
public interface BenchmarkExperimentRepositoryPort {

    BenchmarkExperimentRecord save(BenchmarkExperimentRecord record);

    Optional<BenchmarkExperimentRecord> findById(ExperimentId id);

    List<BenchmarkExperimentRecord> findByScenario(BenchmarkScenarioType scenario);

    List<BenchmarkExperimentRecord> findAll();
}

