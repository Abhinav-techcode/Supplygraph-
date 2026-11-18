package com.supplygraph.adapter.persistence.entity.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_backup_snapshots")
public class BackupSnapshotJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "snapshot_name", length = 128, nullable = false)
    private String snapshotName;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "storage_checksum", length = 128, nullable = false)
    private String storageChecksum;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public BackupSnapshotJpaEntity() {}

    public BackupSnapshotJpaEntity(String id, String workspaceId, String snapshotName, long byteSize, String storageChecksum, boolean verified, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.snapshotName = snapshotName;
        this.byteSize = byteSize;
        this.storageChecksum = storageChecksum;
        this.verified = verified;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getSnapshotName() { return snapshotName; }
    public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }
    public long getByteSize() { return byteSize; }
    public void setByteSize(long byteSize) { this.byteSize = byteSize; }
    public String getStorageChecksum() { return storageChecksum; }
    public void setStorageChecksum(String storageChecksum) { this.storageChecksum = storageChecksum; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

