package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record PoCollaborationId(String value) implements Serializable {
    public PoCollaborationId {
        Objects.requireNonNull(value, "PoCollaborationId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PoCollaborationId cannot be blank");
        }
    }

    public static PoCollaborationId generate() {
        return new PoCollaborationId("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

