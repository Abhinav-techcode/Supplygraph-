package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.platform.WorkspaceQuotaJpaEntity;
import com.supplygraph.adapter.persistence.repository.platform.SpringDataWorkspaceQuotaRepository;
import com.supplygraph.application.platform.port.out.WorkspaceQuotaRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PostgresWorkspaceQuotaPersistenceAdapter implements WorkspaceQuotaRepositoryPort {

    private final SpringDataWorkspaceQuotaRepository repository;

    public PostgresWorkspaceQuotaPersistenceAdapter(SpringDataWorkspaceQuotaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(WorkspaceQuotaRecord record) {
        WorkspaceQuotaJpaEntity entity = new WorkspaceQuotaJpaEntity(
                record.id().value(),
                record.workspaceId().value(),
                record.maxNodes(),
                record.maxActiveDisruptions(),
                record.maxConcurrentSimulations(),
                record.enabled(),
                record.updatedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<WorkspaceQuotaRecord> findById(WorkspaceQuotaId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<WorkspaceQuotaRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).map(this::toDomain);
    }

    private WorkspaceQuotaRecord toDomain(WorkspaceQuotaJpaEntity entity) {
        return new WorkspaceQuotaRecord(
                new WorkspaceQuotaId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getMaxNodes(),
                entity.getMaxActiveDisruptions(),
                entity.getMaxConcurrentSimulations(),
                entity.isEnabled(),
                entity.getUpdatedAt()
        );
    }
}

