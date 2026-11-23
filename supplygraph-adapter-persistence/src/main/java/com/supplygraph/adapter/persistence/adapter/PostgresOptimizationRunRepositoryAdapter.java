package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.OptimizationRunJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataOptimizationRunRepository;
import com.supplygraph.application.port.out.OptimizationRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.simulation.optimization.OptimizationDecision;
import com.supplygraph.simulation.optimization.OptimizationResult;
import com.supplygraph.simulation.optimization.SolverStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresOptimizationRunRepositoryAdapter implements OptimizationRunRepositoryPort {

    private final SpringDataOptimizationRunRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresOptimizationRunRepositoryAdapter(SpringDataOptimizationRunRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(OptimizationResult result, WorkspaceId workspaceId, DisruptionId disruptionId) {
        String decisionsJson = serialize(result.decisions());
        String diagnosticsJson = serialize(result.diagnostics());
        String explanationsJson = serialize(result.explanations());

        OptimizationRunJpaEntity entity = new OptimizationRunJpaEntity(
                result.optimizationRunId(),
                workspaceId.value(),
                disruptionId.value(),
                result.solverStatus().name(),
                result.objectiveValue(),
                result.unmetDemandQuantity(),
                result.expectedDelayDays(),
                result.estimatedCost(),
                result.solverRuntimeMillis(),
                decisionsJson,
                diagnosticsJson,
                explanationsJson,
                Instant.now()
        );

        repository.save(entity);
    }

    @Override
    public Optional<OptimizationResult> findById(String runId) {
        return repository.findById(runId).map(this::toDomain);
    }

    @Override
    public List<OptimizationResult> findByDisruptionId(DisruptionId disruptionId) {
        return repository.findByDisruptionIdOrderByCreatedAtDesc(disruptionId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OptimizationResult> findAllByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByWorkspaceId(WorkspaceId workspaceId) {
        return repository.countByWorkspaceId(workspaceId.value());
    }

    private OptimizationResult toDomain(OptimizationRunJpaEntity entity) {
        List<OptimizationDecision> decisions = deserializeList(entity.getDecisionsJson(), new TypeReference<List<OptimizationDecision>>() {});
        List<String> diagnostics = deserializeStringList(entity.getDiagnosticsJson());
        List<String> explanations = deserializeStringList(entity.getExplanationsJson());

        return new OptimizationResult(
                entity.getId(),
                SolverStatus.valueOf(entity.getSolverStatus()),
                entity.getObjectiveValue(),
                decisions,
                entity.getUnmetDemandQuantity(),
                entity.getExpectedDelayDays(),
                entity.getEstimatedCost(),
                entity.getSolverRuntimeMillis(),
                diagnostics,
                explanations
        );
    }

    private String serialize(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresOptimizationRunRepositoryAdapter)", e);
        }
    }

    private <T> List<T> deserializeList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresOptimizationRunRepositoryAdapter)", e);
        }
    }

    private List<String> deserializeStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresOptimizationRunRepositoryAdapter)", e);
        }
    }
}
