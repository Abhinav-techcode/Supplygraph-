package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.AuditLogJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataAuditLogRepository;
import com.supplygraph.application.port.out.AuditLogRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.audit.AuditLogId;
import com.supplygraph.domain.model.security.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresAuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private final SpringDataAuditLogRepository repository;

    public PostgresAuditLogRepositoryAdapter(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLogEntry save(AuditLogEntry entry) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity(
                entry.id().value(),
                entry.userId().value(),
                entry.workspaceId().value(),
                entry.action(),
                entry.targetEntityId(),
                entry.previousStateVersion(),
                entry.newStateVersion(),
                entry.timestamp(),
                entry.rationale(),
                entry.concurGuardConflictRef(),
                entry.signatureHash()
        );
        AuditLogJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AuditLogEntry> findById(AuditLogId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<AuditLogEntry> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByTimestampDesc(workspaceId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AuditLogEntry> findByTargetEntityId(String targetEntityId) {
        return repository.findByTargetEntityIdOrderByTimestampDesc(targetEntityId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AuditLogEntry> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private AuditLogEntry toDomain(AuditLogJpaEntity entity) {
        return new AuditLogEntry(
                AuditLogId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                entity.getAction(),
                entity.getTargetEntityId(),
                entity.getPreviousStateVersion(),
                entity.getNewStateVersion(),
                entity.getTimestamp(),
                entity.getRationale(),
                entity.getConcurGuardConflictRef(),
                entity.getSignatureHash()
        );
    }
}

