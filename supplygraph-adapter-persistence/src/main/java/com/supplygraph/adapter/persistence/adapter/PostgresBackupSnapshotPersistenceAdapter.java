package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.platform.BackupSnapshotJpaEntity;
import com.supplygraph.adapter.persistence.repository.platform.SpringDataBackupSnapshotRepository;
import com.supplygraph.application.platform.port.out.BackupSnapshotRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.BackupSnapshotId;
import com.supplygraph.domain.model.platform.BackupSnapshotRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresBackupSnapshotPersistenceAdapter implements BackupSnapshotRepositoryPort {

    private final SpringDataBackupSnapshotRepository repository;

    public PostgresBackupSnapshotPersistenceAdapter(SpringDataBackupSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(BackupSnapshotRecord record) {
        BackupSnapshotJpaEntity entity = new BackupSnapshotJpaEntity(
                record.id().value(),
                record.workspaceId().value(),
                record.snapshotName(),
                record.byteSize(),
                record.storageChecksum(),
                record.verified(),
                record.createdAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<BackupSnapshotRecord> findById(BackupSnapshotId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<BackupSnapshotRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private BackupSnapshotRecord toDomain(BackupSnapshotJpaEntity entity) {
        return new BackupSnapshotRecord(
                new BackupSnapshotId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getSnapshotName(),
                entity.getByteSize(),
                entity.getStorageChecksum(),
                entity.isVerified(),
                entity.getCreatedAt()
        );
    }
}

