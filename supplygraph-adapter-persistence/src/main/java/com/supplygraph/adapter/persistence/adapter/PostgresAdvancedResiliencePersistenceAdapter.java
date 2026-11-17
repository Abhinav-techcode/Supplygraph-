package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.AdvancedResilienceMetricsJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataAdvancedResilienceRepository;
import com.supplygraph.application.port.out.AdvancedResilienceRepositoryPort;
import com.supplygraph.domain.model.resilience.advanced.AdvancedResilienceMetrics;
import com.supplygraph.domain.model.resilience.advanced.NodeResilienceMetric;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresAdvancedResiliencePersistenceAdapter implements AdvancedResilienceRepositoryPort {

    private final SpringDataAdvancedResilienceRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresAdvancedResiliencePersistenceAdapter(SpringDataAdvancedResilienceRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(AdvancedResilienceMetrics metrics) {
        String breakdownJson = serialize(metrics.nodeResilienceBreakdown());
        AdvancedResilienceMetricsJpaEntity entity = new AdvancedResilienceMetricsJpaEntity(
                metrics.assessmentId(),
                metrics.workspaceId(),
                metrics.timeToSurviveDays(),
                metrics.timeToRecoverDays(),
                metrics.criticalSupplierExposureIndex(),
                metrics.recoveryFlexibilityIndex(),
                metrics.bottleneckNodeCount(),
                metrics.compositeResilienceScore(),
                breakdownJson,
                metrics.calculatedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<AdvancedResilienceMetrics> findLatestByWorkspaceId(String workspaceId) {
        return repository.findTopByWorkspaceIdOrderByCalculatedAtDesc(workspaceId).map(this::toDomain);
    }

    @Override
    public List<AdvancedResilienceMetrics> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private AdvancedResilienceMetrics toDomain(AdvancedResilienceMetricsJpaEntity e) {
        List<NodeResilienceMetric> breakdown = deserialize(e.getBreakdownJson(), new TypeReference<List<NodeResilienceMetric>>() {}, List.of());
        return AdvancedResilienceMetrics.of(
                e.getId(),
                e.getWorkspaceId(),
                e.getTimeToSurviveDays(),
                e.getTimeToRecoverDays(),
                e.getSupplierExposureIdx(),
                e.getRecoveryFlexibilityIdx(),
                e.getBottleneckNodeCount(),
                e.getCompositeResilienceScore(),
                breakdown,
                e.getCalculatedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef, T defaultVal) {
        if (json == null || json.isBlank()) return defaultVal;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
