package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record RfqId(String value) implements Serializable {
    public RfqId {
        Objects.requireNonNull(value, "RfqId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RfqId cannot be blank");
        }
    }

    public static RfqId generate() {
        return new RfqId("RFQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

