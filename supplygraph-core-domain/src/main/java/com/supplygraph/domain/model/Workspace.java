package com.supplygraph.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Root workspace aggregate representing an isolated multi-tier supply network tenant.
 * Pure Java record with zero external framework dependencies.
 */
public record Workspace(
        WorkspaceId id,
        String name,
        String industry,
        Instant createdAt,
        Instant updatedAt
) {
    public Workspace {
        Objects.requireNonNull(id, "Workspace id must not be null");
        Objects.requireNonNull(name, "Workspace name must not be null");
        Objects.requireNonNull(industry, "Workspace industry must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Workspace name must not be blank");
        }
        if (industry.isBlank()) {
            throw new IllegalArgumentException("Workspace industry must not be blank");
        }
    }

    public static Workspace create(WorkspaceId id, String name, String industry) {
        Instant now = Instant.now();
        return new Workspace(id, name, industry, now, now);
    }
}

