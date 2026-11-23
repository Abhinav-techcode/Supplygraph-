package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.RecoveryPlanJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataRecoveryPlanRepository;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresRecoveryPlanRepositoryAdapter implements RecoveryPlanRepositoryPort {

    private final SpringDataRecoveryPlanRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresRecoveryPlanRepositoryAdapter(SpringDataRecoveryPlanRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(RecoveryPlan plan) {
        String actionsJson = serialize(plan.actions());
        String metricsJson = serialize(plan.metrics());
        String conflictsJson = serialize(plan.concurGuardConflictIds());
        String approvalJson = serialize(plan.approval());

        RecoveryPlanJpaEntity entity = new RecoveryPlanJpaEntity(
                plan.id().value(),
                plan.disruptionId().value(),
                plan.workspaceId().value(),
                plan.sourceTwinVersion().value(),
                plan.impactAssessmentId() != null ? plan.impactAssessmentId().value() : null,
                plan.simulationRunId(),
                plan.monteCarloRunId(),
                plan.optimizationRunId(),
                plan.planName(),
                plan.description(),
                plan.status().name(),
                actionsJson,
                metricsJson,
                plan.concurGuardValidated(),
                conflictsJson,
                approvalJson,
                plan.createdAt(),
                plan.updatedAt(),
                plan.version()
        );

        repository.save(entity);
    }

    @Override
    public Optional<RecoveryPlan> findById(RecoveryPlanId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<RecoveryPlan> findByDisruptionId(DisruptionId disruptionId) {
        return repository.findByDisruptionIdOrderByCreatedAtDesc(disruptionId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RecoveryPlan> findAllByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countPendingApprovalByWorkspaceId(WorkspaceId workspaceId) {
        return repository.countByWorkspaceIdAndStatus(workspaceId.value(), RecoveryPlanStatus.PENDING_APPROVAL.name());
    }

    private RecoveryPlan toDomain(RecoveryPlanJpaEntity entity) {
        List<PlanAction> actions = deserializeList(entity.getActionsJson(), new TypeReference<List<PlanAction>>() {});
        PlanComparisonMetrics metrics = deserializeMetrics(entity.getMetricsJson());
        List<String> conflicts = deserializeList(entity.getConcurGuardConflictsJson(), new TypeReference<List<String>>() {});
        PlanApproval approval = deserializeApproval(entity.getApprovalJson());

        ImpactAssessmentId assessmentId = entity.getImpactAssessmentId() != null
                ? ImpactAssessmentId.of(entity.getImpactAssessmentId())
                : null;

        return new RecoveryPlan(
                RecoveryPlanId.of(entity.getId()),
                DisruptionId.of(entity.getDisruptionId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                StateVersion.of(entity.getSourceTwinVersion()),
                assessmentId,
                entity.getSimulationRunId(),
                entity.getMonteCarloRunId(),
                entity.getOptimizationRunId(),
                entity.getPlanName(),
                entity.getDescription(),
                RecoveryPlanStatus.valueOf(entity.getStatus()),
                actions,
                metrics,
                entity.isConcurGuardValidated(),
                conflicts,
                approval,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresRecoveryPlanRepositoryAdapter)", e);
        }
    }

    private <T> List<T> deserializeList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresRecoveryPlanRepositoryAdapter)", e);
        }
    }

    private PlanComparisonMetrics deserializeMetrics(String json) {
        if (json == null || json.isBlank()) return PlanComparisonMetrics.defaultMetrics();
        try {
            return objectMapper.readValue(json, PlanComparisonMetrics.class);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresRecoveryPlanRepositoryAdapter)", e);
        }
    }

    private PlanApproval deserializeApproval(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, PlanApproval.class);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresRecoveryPlanRepositoryAdapter)", e);
        }
    }
}
