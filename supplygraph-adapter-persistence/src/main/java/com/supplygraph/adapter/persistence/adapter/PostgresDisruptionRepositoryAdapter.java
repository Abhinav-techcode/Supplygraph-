package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.DisruptionJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataDisruptionRepository;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PostgresDisruptionRepositoryAdapter implements DisruptionRepositoryPort {

    private final SpringDataDisruptionRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresDisruptionRepositoryAdapter(SpringDataDisruptionRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(Disruption disruption) {
        String nodesJson = serialize(disruption.affectedNodeIds().stream().map(NodeId::value).toList());
        String lanesJson = serialize(disruption.affectedLaneIds().stream().map(EdgeId::value).toList());
        String matsJson = serialize(disruption.affectedMaterialIds().stream().map(MaterialId::value).toList());
        String paramsJson = serialize(disruption.impactParameters());

        DisruptionJpaEntity entity = new DisruptionJpaEntity(
                disruption.id().value(),
                disruption.workspaceId().value(),
                disruption.industryCode(),
                disruption.scenarioType(),
                disruption.title(),
                disruption.description(),
                disruption.category().name(),
                disruption.severity().name(),
                disruption.status().name(),
                nodesJson,
                lanesJson,
                matsJson,
                disruption.startTime(),
                disruption.expectedEndTime(),
                paramsJson,
                disruption.sourceEvidence(),
                disruption.createdAt(),
                disruption.updatedAt(),
                disruption.version()
        );

        repository.save(entity);
    }

    @Override
    public Optional<Disruption> findById(DisruptionId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Disruption> findAllByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Disruption> findByWorkspaceIdAndStatus(WorkspaceId workspaceId, DisruptionStatus status) {
        return repository.findByWorkspaceIdAndStatus(workspaceId.value(), status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countActiveByWorkspaceId(WorkspaceId workspaceId) {
        return repository.countByWorkspaceIdAndStatus(workspaceId.value(), DisruptionStatus.ACTIVE.name());
    }

    private Disruption toDomain(DisruptionJpaEntity entity) {
        List<String> nodeStrs = deserializeList(entity.getAffectedNodesJson());
        List<String> laneStrs = deserializeList(entity.getAffectedLanesJson());
        List<String> matStrs = deserializeList(entity.getAffectedMaterialsJson());

        Set<NodeId> nodes = nodeStrs.stream().map(NodeId::of).collect(Collectors.toSet());
        Set<EdgeId> lanes = laneStrs.stream().map(EdgeId::of).collect(Collectors.toSet());
        Set<MaterialId> mats = matStrs.stream().map(MaterialId::of).collect(Collectors.toSet());

        DisruptionImpactParameters params = deserializeParams(entity.getImpactParamsJson());

        return new Disruption(
                DisruptionId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                entity.getIndustryCode(),
                entity.getScenarioType(),
                entity.getTitle(),
                entity.getDescription(),
                DisruptionCategory.valueOf(entity.getCategory()),
                DisruptionSeverity.valueOf(entity.getSeverity()),
                DisruptionStatus.valueOf(entity.getStatus()),
                nodes,
                lanes,
                mats,
                entity.getStartTime(),
                entity.getExpectedEndTime(),
                params,
                entity.getSourceEvidence(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
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

    private DisruptionImpactParameters deserializeParams(String json) {
        if (json == null || json.isBlank()) return DisruptionImpactParameters.empty();
        try {
            return objectMapper.readValue(json, DisruptionImpactParameters.class);
        } catch (Exception e) {
            return DisruptionImpactParameters.empty();
        }
    }
}
