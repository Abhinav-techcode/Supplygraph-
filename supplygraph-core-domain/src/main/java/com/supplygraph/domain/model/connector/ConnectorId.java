package com.supplygraph.domain.model.connector;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record ConnectorId(String value) implements Serializable {
    public ConnectorId {
        Objects.requireNonNull(value, "ConnectorId must not be null");
        if (value.isBlank()) throw new IllegalArgumentException("ConnectorId must not be blank");
    }
    public static ConnectorId generate() { return new ConnectorId("CON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()); }
    public static ConnectorId of(String v) { return new ConnectorId(v); }
    @Override public String toString() { return value; }
}
