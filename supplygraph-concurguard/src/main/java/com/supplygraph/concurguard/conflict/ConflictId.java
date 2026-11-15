package com.supplygraph.concurguard.conflict;

import java.util.Objects;
import java.util.UUID;

public record ConflictId(String value) {
    public ConflictId {
        Objects.requireNonNull(value, "ConflictId must not be null");
    }

    public static ConflictId generate() {
        return new ConflictId(UUID.randomUUID().toString());
    }

    public static ConflictId of(String value) {
        return new ConflictId(value);
    }
}
