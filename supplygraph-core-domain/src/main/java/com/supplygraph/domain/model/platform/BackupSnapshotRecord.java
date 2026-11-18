package com.supplygraph.domain.model.platform;

import com.supplygraph.domain.model.WorkspaceId;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record BackupSnapshotRecord(
        BackupSnapshotId id,
        WorkspaceId workspaceId,
        String snapshotName,
        long byteSize,
        String storageChecksum,
        boolean verified,
        Instant createdAt
) implements Serializable {
    public BackupSnapshotRecord {
        Objects.requireNonNull(id, "BackupSnapshotId cannot be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        Objects.requireNonNull(snapshotName, "SnapshotName cannot be null");
        Objects.requireNonNull(storageChecksum, "StorageChecksum cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        if (byteSize < 0) {
            throw new IllegalArgumentException("ByteSize must be non-negative");
        }
    }

    public BackupSnapshotRecord withVerified(boolean newVerified) {
        return new BackupSnapshotRecord(id, workspaceId, snapshotName, byteSize, storageChecksum, newVerified, createdAt);
    }
}

