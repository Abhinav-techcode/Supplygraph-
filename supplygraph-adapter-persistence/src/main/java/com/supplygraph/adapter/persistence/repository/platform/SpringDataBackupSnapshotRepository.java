package com.supplygraph.adapter.persistence.repository.platform;

import com.supplygraph.adapter.persistence.entity.platform.BackupSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataBackupSnapshotRepository extends JpaRepository<BackupSnapshotJpaEntity, String> {
    List<BackupSnapshotJpaEntity> findByWorkspaceId(String workspaceId);
}

