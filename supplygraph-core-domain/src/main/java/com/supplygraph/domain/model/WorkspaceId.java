package com.supplygraph.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for a SupplyGraph workspace.
 */
public record WorkspaceId(String value) implements Serializable {
    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkspaceId value must not be blank");
        }
    }

    public static WorkspaceId of(String value) {
        return new WorkspaceId(value);
    }
}

