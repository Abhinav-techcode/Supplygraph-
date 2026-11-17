package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.SimulationRunJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataSimulationRunRepository;
import com.supplygraph.application.port.out.SimulationRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.engine.SimulationResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresSimulationRunRepositoryAdapter implements SimulationRunRepositoryPort {

    private final SpringDataSimulationRunRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresSimulationRunRepositoryAdapter(SpringDataSimulationRunRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(SimulationResult result, WorkspaceId workspaceId, DisruptionId disruptionId, String planId) {
        String warningsJson = serialize(result.warnings());
        String assumptionsJson = serialize(result.assumptions());

        SimulationRunJpaEntity entity = new SimulationRunJpaEntity(
                result.simulationId(),
                workspaceId.value(),
                disruptionId.value(),
                planId,
                result.scenarioId(),
                result.sourceTwinVersion().value(),
                result.startTime(),
                result.endTime(),
                result.processedEventCount(),
                result.serviceLevelPercentage(),
                result.totalShortageQuantity(),
                result.maxDelaySeconds(),
                result.status(),
                warningsJson,
                assumptionsJson,
                Instant.now()
        );

        repository.save(entity);
    }

    @Override
    public Optional<SimulationResult> findById(String simulationId) {
        return repository.findById(simulationId).map(this::toDomain);
    }

    @Override
    public List<SimulationResult> findByDisruptionId(DisruptionId disruptionId) {
        return repository.findByDisruptionIdOrderByCreatedAtDesc(disruptionId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SimulationResult> findAllByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByWorkspaceId(WorkspaceId workspaceId) {
        return repository.countByWorkspaceId(workspaceId.value());
    }

    private SimulationResult toDomain(SimulationRunJpaEntity entity) {
        List<String> warnings = deserializeList(entity.getWarningsJson());
        List<String> assumptions = deserializeList(entity.getAssumptionsJson());

        return new SimulationResult(
                entity.getId(),
                entity.getScenarioId(),
                StateVersion.of(entity.getSourceTwinVersion()),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getProcessedEventCount(),
                List.of(), // Timeline events loaded on demand or summarized
                List.of(),
                List.of(),
                List.of(),
                entity.getServiceLevelPercentage(),
                entity.getTotalShortageQuantity(),
                entity.getMaxDelaySeconds(),
                entity.getStatus(),
                warnings,
                assumptions
        );
    }

    private String serialize(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
