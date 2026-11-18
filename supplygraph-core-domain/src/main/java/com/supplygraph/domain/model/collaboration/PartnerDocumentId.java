package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record PartnerDocumentId(String value) implements Serializable {
    public PartnerDocumentId {
        Objects.requireNonNull(value, "PartnerDocumentId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PartnerDocumentId cannot be blank");
        }
    }

    public static PartnerDocumentId generate() {
        return new PartnerDocumentId("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

