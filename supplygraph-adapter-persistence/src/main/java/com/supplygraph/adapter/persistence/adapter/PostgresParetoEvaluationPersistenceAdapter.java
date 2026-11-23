package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ParetoEvaluationJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataParetoEvaluationRepository;
import com.supplygraph.application.port.out.ParetoEvaluationRepositoryPort;
import com.supplygraph.domain.model.pareto.ParetoFrontier;
import com.supplygraph.domain.model.pareto.ParetoSolution;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresParetoEvaluationPersistenceAdapter implements ParetoEvaluationRepositoryPort {

    private final SpringDataParetoEvaluationRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresParetoEvaluationPersistenceAdapter(SpringDataParetoEvaluationRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(ParetoFrontier frontier) {
        String allJson = serialize(frontier.allSolutions());
        String frontierJson = serialize(frontier.frontierSolutions());

        ParetoEvaluationJpaEntity entity = new ParetoEvaluationJpaEntity(
                frontier.evaluationId(),
                frontier.disruptionId(),
                allJson,
                frontierJson,
                frontier.evaluatedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<ParetoFrontier> findByEvaluationId(String evaluationId) {
        return repository.findById(evaluationId).map(this::toDomain);
    }

    @Override
    public Optional<ParetoFrontier> findLatestByDisruptionId(String disruptionId) {
        return repository.findTopByDisruptionIdOrderByEvaluatedAtDesc(disruptionId).map(this::toDomain);
    }

    @Override
    public List<ParetoFrontier> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private ParetoFrontier toDomain(ParetoEvaluationJpaEntity e) {
        List<ParetoSolution> all = deserialize(e.getAllSolutionsJson(), new TypeReference<List<ParetoSolution>>() {}, List.of());
        List<ParetoSolution> frontier = deserialize(e.getFrontierSolutionsJson(), new TypeReference<List<ParetoSolution>>() {}, List.of());

        return new ParetoFrontier(
                e.getId(),
                e.getDisruptionId(),
                all,
                frontier,
                e.getEvaluatedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresParetoEvaluationPersistenceAdapter)", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef, T defaultVal) {
        if (json == null || json.isBlank()) return defaultVal;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresParetoEvaluationPersistenceAdapter)", e);
        }
    }
}

