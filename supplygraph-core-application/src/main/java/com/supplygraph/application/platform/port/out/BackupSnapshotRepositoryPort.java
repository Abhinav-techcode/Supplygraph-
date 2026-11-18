package com.supplygraph.application.platform.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.BackupSnapshotId;
import com.supplygraph.domain.model.platform.BackupSnapshotRecord;

import java.util.List;
import java.util.Optional;

public interface BackupSnapshotRepositoryPort {
    void save(BackupSnapshotRecord record);
    Optional<BackupSnapshotRecord> findById(BackupSnapshotId id);
    List<BackupSnapshotRecord> findByWorkspaceId(WorkspaceId workspaceId);
}

