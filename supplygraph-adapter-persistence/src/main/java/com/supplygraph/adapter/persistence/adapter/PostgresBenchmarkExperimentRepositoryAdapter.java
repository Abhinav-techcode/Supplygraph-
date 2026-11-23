package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.BenchmarkExperimentJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataBenchmarkExperimentRepository;
import com.supplygraph.application.port.out.BenchmarkExperimentRepositoryPort;
import com.supplygraph.domain.model.benchmark.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresBenchmarkExperimentRepositoryAdapter implements BenchmarkExperimentRepositoryPort {

    private final SpringDataBenchmarkExperimentRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresBenchmarkExperimentRepositoryAdapter(SpringDataBenchmarkExperimentRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public BenchmarkExperimentRecord save(BenchmarkExperimentRecord record) {
        String metricsJson = serialize(record.metrics());
        BenchmarkExperimentJpaEntity entity = new BenchmarkExperimentJpaEntity(
                record.id().value(),
                record.name(),
                record.scenario().name(),
                record.baseline().name(),
                record.datasetReference(),
                record.twinVersion(),
                record.randomSeed(),
                record.trialRuns(),
                record.executedAt(),
                metricsJson,
                record.status(),
                record.limitationsNotes()
        );
        BenchmarkExperimentJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BenchmarkExperimentRecord> findById(ExperimentId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<BenchmarkExperimentRecord> findByScenario(BenchmarkScenarioType scenario) {
        return repository.findByScenarioOrderByExecutedAtDesc(scenario.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<BenchmarkExperimentRecord> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private BenchmarkExperimentRecord toDomain(BenchmarkExperimentJpaEntity entity) {
        BenchmarkMetrics metrics = deserialize(entity.getMetricsJson(), new TypeReference<BenchmarkMetrics>() {});
        return new BenchmarkExperimentRecord(
                ExperimentId.of(entity.getId()),
                entity.getName(),
                BenchmarkScenarioType.valueOf(entity.getScenario()),
                BenchmarkBaseline.valueOf(entity.getBaseline()),
                entity.getDatasetReference(),
                entity.getTwinVersion(),
                entity.getRandomSeed(),
                entity.getTrialRuns(),
                entity.getExecutedAt(),
                metrics != null ? metrics : BenchmarkMetrics.empty(),
                entity.getStatus(),
                entity.getLimitationsNotes()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresBenchmarkExperimentRepositoryAdapter)", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> ref) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, ref);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresBenchmarkExperimentRepositoryAdapter)", e);
        }
    }
}

