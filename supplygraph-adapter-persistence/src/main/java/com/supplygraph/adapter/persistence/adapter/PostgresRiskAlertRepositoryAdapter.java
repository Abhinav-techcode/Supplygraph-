package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.RiskAlertJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataRiskAlertRepository;
import com.supplygraph.application.port.out.RiskAlertRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.monitoring.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresRiskAlertRepositoryAdapter implements RiskAlertRepositoryPort {

    private final SpringDataRiskAlertRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresRiskAlertRepositoryAdapter(SpringDataRiskAlertRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(RiskAlert alert) {
        String evidenceJson = serialize(alert.getEvidence());

        RiskAlertJpaEntity entity = new RiskAlertJpaEntity(
                alert.getId().value(),
                alert.getWorkspaceId().value(),
                alert.getRuleId().value(),
                alert.getSeverity().name(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getAffectedEntityId(),
                alert.getAffectedEntityType(),
                alert.getTriggerEventId() != null ? alert.getTriggerEventId().value() : null,
                alert.getSourceStateVersion().value(),
                evidenceJson,
                alert.getSuppressionKey(),
                alert.getStatus().name(),
                alert.getDetectedAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<RiskAlert> findById(RiskAlertId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<RiskAlert> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByDetectedAtDesc(workspaceId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<RiskAlert> findActiveAlerts(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdAndStatusOrderByDetectedAtDesc(workspaceId.value(), RiskAlertStatus.ACTIVE.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<RiskAlert> findActiveBySuppressionKey(String suppressionKey) {
        return repository.findBySuppressionKeyAndStatus(suppressionKey, RiskAlertStatus.ACTIVE.name())
                .map(this::toDomain);
    }

    private RiskAlert toDomain(RiskAlertJpaEntity entity) {
        Map<String, String> evidence = deserialize(entity.getEvidenceJson(), new TypeReference<Map<String, String>>() {});

        return new RiskAlert(
                RiskAlertId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                MonitoringRuleId.of(entity.getRuleId()),
                MonitoringSeverity.valueOf(entity.getSeverity()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getAffectedEntityId(),
                entity.getAffectedEntityType(),
                entity.getTriggerEventId() != null ? EventId.of(entity.getTriggerEventId()) : null,
                StateVersion.of(entity.getSourceStateVersion()),
                evidence != null ? evidence : Map.of(),
                entity.getSuppressionKey(),
                RiskAlertStatus.valueOf(entity.getStatus()),
                entity.getDetectedAt(),
                entity.getAcknowledgedAt(),
                entity.getResolvedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T deserialize(String json, TypeReference<T> ref) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, ref);
        } catch (Exception e) {
            return null;
        }
    }
}

