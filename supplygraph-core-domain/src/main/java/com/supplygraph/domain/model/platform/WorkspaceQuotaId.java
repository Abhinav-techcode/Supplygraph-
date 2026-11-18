package com.supplygraph.domain.model.platform;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record WorkspaceQuotaId(String value) implements Serializable {
    public WorkspaceQuotaId {
        Objects.requireNonNull(value, "WorkspaceQuotaId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkspaceQuotaId cannot be blank");
        }
    }

    public static WorkspaceQuotaId generate() {
        return new WorkspaceQuotaId("QUOTA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

