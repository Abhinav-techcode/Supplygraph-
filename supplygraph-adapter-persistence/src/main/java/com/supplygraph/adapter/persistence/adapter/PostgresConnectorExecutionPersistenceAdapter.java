package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.ConnectorExecutionJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataConnectorExecutionRepository;
import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresConnectorExecutionPersistenceAdapter implements ConnectorExecutionRepositoryPort {
    private final SpringDataConnectorExecutionRepository repository;

    public PostgresConnectorExecutionPersistenceAdapter(SpringDataConnectorExecutionRepository repository) { this.repository = repository; }

    @Override @Transactional
    public ConnectorExecution save(ConnectorExecution x) {
        ConnectorExecutionJpaEntity e = repository.findById(x.id()).orElseGet(ConnectorExecutionJpaEntity::new);
        e.setId(x.id());
        e.setConnectorId(x.connectorId().value());
        e.setWorkspaceId(x.workspaceId().value());
        e.setOperation(x.operation().name());
        e.setTriggerType(x.trigger().name());
        e.setStatus(x.status().name());
        e.setStartedAt(x.startedAt());
        e.setFinishedAt(x.finishedAt());
        e.setRecordsReceived(x.recordsReceived());
        e.setRecordsProcessed(x.recordsProcessed());
        e.setRecordsRejected(x.recordsRejected());
        e.setErrorCategory(x.errorCategory() != null ? x.errorCategory().name() : null);
        e.setErrorMessage(x.errorMessage());
        e.setAttempt(x.attempt());
        e.setRetryOf(x.retryOf());
        e.setCorrelationId(x.correlationId());
        e.setSimulated(x.simulated());
        e.setRequestedBy(x.requestedBy());
        return toDomain(repository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public Optional<ConnectorExecution> findById(String id) { return repository.findById(id).map(this::toDomain); }

    @Override @Transactional(readOnly = true)
    public List<ConnectorExecution> findByConnector(ConnectorId id, int limit) {
        return repository.findByConnectorIdOrderByStartedAtDesc(id.value(), PageRequest.of(0, bound(limit))).stream().map(this::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ConnectorExecution> findByWorkspace(WorkspaceId ws, int limit) {
        return repository.findByWorkspaceIdOrderByStartedAtDesc(ws.value(), PageRequest.of(0, bound(limit))).stream().map(this::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public boolean hasRetry(String executionId) { return repository.existsByRetryOf(executionId); }

    private static int bound(int limit) { return Math.min(Math.max(limit, 1), 500); }

    private ConnectorExecution toDomain(ConnectorExecutionJpaEntity e) {
        return new ConnectorExecution(e.getId(), ConnectorId.of(e.getConnectorId()), WorkspaceId.of(e.getWorkspaceId()),
                ConnectorOperation.valueOf(e.getOperation()), ExecutionTrigger.valueOf(e.getTriggerType()), ExecutionStatus.valueOf(e.getStatus()),
                e.getStartedAt(), e.getFinishedAt(), e.getRecordsReceived(), e.getRecordsProcessed(), e.getRecordsRejected(),
                e.getErrorCategory() != null ? ErrorCategory.valueOf(e.getErrorCategory()) : null, e.getErrorMessage(),
                e.getAttempt(), e.getRetryOf(), e.getCorrelationId(), e.isSimulated(), e.getRequestedBy());
    }
}
