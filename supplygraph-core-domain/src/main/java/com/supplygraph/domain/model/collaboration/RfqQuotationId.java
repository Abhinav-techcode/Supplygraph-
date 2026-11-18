package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record RfqQuotationId(String value) implements Serializable {
    public RfqQuotationId {
        Objects.requireNonNull(value, "RfqQuotationId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RfqQuotationId cannot be blank");
        }
    }

    public static RfqQuotationId generate() {
        return new RfqQuotationId("QUOTE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

