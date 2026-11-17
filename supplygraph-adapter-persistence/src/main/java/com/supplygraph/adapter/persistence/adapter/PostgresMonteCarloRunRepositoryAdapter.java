package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.MonteCarloRunJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataMonteCarloRunRepository;
import com.supplygraph.application.port.out.MonteCarloRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.simulation.montecarlo.MonteCarloSummaryResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresMonteCarloRunRepositoryAdapter implements MonteCarloRunRepositoryPort {

    private final SpringDataMonteCarloRunRepository repository;

    public PostgresMonteCarloRunRepositoryAdapter(SpringDataMonteCarloRunRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(MonteCarloSummaryResult result, WorkspaceId workspaceId, DisruptionId disruptionId, String planId) {
        MonteCarloRunJpaEntity entity = new MonteCarloRunJpaEntity(
                result.monteCarloRunId(),
                workspaceId.value(),
                disruptionId.value(),
                planId,
                result.seed(),
                result.replicationCount(),
                result.meanDelayDays(),
                result.medianDelayDays(),
                result.p90DelayDays(),
                result.p95DelayDays(),
                result.meanShortageQuantity(),
                result.maxShortageQuantity(),
                result.slaViolationProbability(),
                result.recoveryWithinDeadlineProbability(),
                result.estimatedMeanCost(),
                result.status(),
                Instant.now()
        );

        repository.save(entity);
    }

    @Override
    public Optional<MonteCarloSummaryResult> findById(String runId) {
        return repository.findById(runId).map(this::toDomain);
    }

    @Override
    public List<MonteCarloSummaryResult> findByDisruptionId(DisruptionId disruptionId) {
        return repository.findByDisruptionIdOrderByCreatedAtDesc(disruptionId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MonteCarloSummaryResult> findAllByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByWorkspaceId(WorkspaceId workspaceId) {
        return repository.countByWorkspaceId(workspaceId.value());
    }

    private MonteCarloSummaryResult toDomain(MonteCarloRunJpaEntity entity) {
        return new MonteCarloSummaryResult(
                entity.getId(),
                entity.getSeed(),
                entity.getReplicationCount(),
                entity.getMeanDelayDays(),
                entity.getMedianDelayDays(),
                entity.getP90DelayDays(),
                entity.getP95DelayDays(),
                entity.getMeanShortageQuantity(),
                entity.getMaxShortageQuantity(),
                entity.getSlaViolationProbability(),
                entity.getRecoveryWithinDeadlineProbability(),
                entity.getEstimatedMeanCost(),
                entity.getStatus(),
                List.of()
        );
    }
}

