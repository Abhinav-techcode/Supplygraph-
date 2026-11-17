package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ReplanningRunJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataReplanningRunRepository;
import com.supplygraph.application.port.out.ReplanningRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.monitoring.RiskAlertId;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningRunId;
import com.supplygraph.domain.model.replanning.ReplanningStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresReplanningRunRepositoryAdapter implements ReplanningRunRepositoryPort {

    private final SpringDataReplanningRunRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresReplanningRunRepositoryAdapter(SpringDataReplanningRunRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(ReplanningRun run) {
        String agentsJson = serialize(run.getAffectedAgentIds());
        String preservedJson = serialize(run.getPreservedActionIds());
        String reconsideredJson = serialize(run.getReconsideredActionIds());
        String auditJson = serialize(run.getAuditTrail());

        ReplanningRunJpaEntity entity = new ReplanningRunJpaEntity(
                run.getId().value(),
                run.getWorkspaceId().value(),
                run.getTriggerAlertId() != null ? run.getTriggerAlertId().value() : null,
                run.getSupersededPlanId() != null ? run.getSupersededPlanId().value() : null,
                run.getRevisedPlanId() != null ? run.getRevisedPlanId().value() : null,
                run.getStatus().name(),
                agentsJson,
                preservedJson,
                reconsideredJson,
                run.getRetryCount(),
                run.getMaxRetries(),
                run.getReason(),
                auditJson,
                run.getStartedAt(),
                run.getCompletedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<ReplanningRun> findById(ReplanningRunId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ReplanningRun> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByStartedAtDesc(workspaceId.value())
                .stream().map(this::toDomain).toList();
    }

    private ReplanningRun toDomain(ReplanningRunJpaEntity entity) {
        Set<String> agents = deserialize(entity.getAffectedAgentIds(), new TypeReference<Set<String>>() {});
        Set<String> preserved = deserialize(entity.getPreservedActionIds(), new TypeReference<Set<String>>() {});
        Set<String> reconsidered = deserialize(entity.getReconsideredActionIds(), new TypeReference<Set<String>>() {});
        List<String> audit = deserialize(entity.getAuditTrailJson(), new TypeReference<List<String>>() {});

        return new ReplanningRun(
                ReplanningRunId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                entity.getTriggerAlertId() != null ? RiskAlertId.of(entity.getTriggerAlertId()) : null,
                entity.getSupersededPlanId() != null ? RecoveryPlanId.of(entity.getSupersededPlanId()) : null,
                entity.getRevisedPlanId() != null ? RecoveryPlanId.of(entity.getRevisedPlanId()) : null,
                ReplanningStatus.valueOf(entity.getStatus()),
                agents != null ? agents : Set.of(),
                preserved != null ? preserved : Set.of(),
                reconsidered != null ? reconsidered : Set.of(),
                entity.getRetryCount(),
                entity.getMaxRetries(),
                entity.getReason(),
                audit != null ? audit : List.of(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
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

