package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ImpactAssessmentJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataImpactAssessmentRepository;
import com.supplygraph.application.port.out.ImpactAssessmentRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.impact.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresImpactAssessmentRepositoryAdapter implements ImpactAssessmentRepositoryPort {

    private final SpringDataImpactAssessmentRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresImpactAssessmentRepositoryAdapter(SpringDataImpactAssessmentRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(ImpactAssessment assessment) {
        String nodesJson = serialize(assessment.nodeImpacts());
        String matsJson = serialize(assessment.materialShortages());
        String capsJson = serialize(assessment.capacityImpacts());
        String logsJson = serialize(assessment.logisticsImpacts());
        String risksJson = serialize(assessment.riskIndicators());
        String assumptionsJson = serialize(assessment.assumptions());

        ImpactAssessmentJpaEntity entity = new ImpactAssessmentJpaEntity(
                assessment.id().value(),
                assessment.disruptionId().value(),
                assessment.workspaceId().value(),
                assessment.digitalTwinVersion().value(),
                assessment.assessmentTimestamp(),
                assessment.overallSeverity().name(),
                nodesJson,
                matsJson,
                capsJson,
                logsJson,
                assessment.totalEstimatedCost(),
                risksJson,
                assumptionsJson,
                assessment.status(),
                assessment.version()
        );

        repository.save(entity);
    }

    @Override
    public Optional<ImpactAssessment> findById(ImpactAssessmentId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<ImpactAssessment> findLatestByDisruptionId(DisruptionId disruptionId) {
        return repository.findFirstByDisruptionIdOrderByAssessmentTimestampDesc(disruptionId.value()).map(this::toDomain);
    }

    @Override
    public List<ImpactAssessment> findAllByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByAssessmentTimestampDesc(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByWorkspaceId(WorkspaceId workspaceId) {
        return repository.countByWorkspaceId(workspaceId.value());
    }

    private ImpactAssessment toDomain(ImpactAssessmentJpaEntity entity) {
        List<NodeImpact> nodes = deserializeList(entity.getNodeImpactsJson(), new TypeReference<List<NodeImpact>>() {});
        List<MaterialShortageImpact> mats = deserializeList(entity.getMaterialShortagesJson(), new TypeReference<List<MaterialShortageImpact>>() {});
        List<CapacityImpact> caps = deserializeList(entity.getCapacityImpactsJson(), new TypeReference<List<CapacityImpact>>() {});
        List<LogisticsImpact> logs = deserializeList(entity.getLogisticsImpactsJson(), new TypeReference<List<LogisticsImpact>>() {});
        List<String> risks = deserializeList(entity.getRiskIndicatorsJson(), new TypeReference<List<String>>() {});
        List<String> assumptions = deserializeList(entity.getAssumptionsJson(), new TypeReference<List<String>>() {});

        return new ImpactAssessment(
                ImpactAssessmentId.of(entity.getId()),
                DisruptionId.of(entity.getDisruptionId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                StateVersion.of(entity.getDigitalTwinVersion()),
                entity.getAssessmentTimestamp(),
                ImpactSeverity.valueOf(entity.getOverallSeverity()),
                nodes,
                mats,
                caps,
                logs,
                entity.getTotalEstimatedCost(),
                risks,
                assumptions,
                List.of(),
                entity.getStatus(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    private String serialize(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresImpactAssessmentRepositoryAdapter)", e);
        }
    }

    private <T> List<T> deserializeList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresImpactAssessmentRepositoryAdapter)", e);
        }
    }
}
