package com.supplygraph.domain.model.audit;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed value object representing an audit log identifier.
 */
public record AuditLogId(String value) implements Serializable {

    public AuditLogId {
        Objects.requireNonNull(value, "Audit log ID value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Audit log ID cannot be blank");
        }
    }

    public static AuditLogId generate() {
        return new AuditLogId("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public static AuditLogId of(String value) {
        return new AuditLogId(value.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}

