package com.supplygraph.domain.model.platform;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record BackupSnapshotId(String value) implements Serializable {
    public BackupSnapshotId {
        Objects.requireNonNull(value, "BackupSnapshotId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("BackupSnapshotId cannot be blank");
        }
    }

    public static BackupSnapshotId generate() {
        return new BackupSnapshotId("SNAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

