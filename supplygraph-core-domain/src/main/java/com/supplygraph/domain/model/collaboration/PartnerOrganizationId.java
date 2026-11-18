package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record PartnerOrganizationId(String value) implements Serializable {
    public PartnerOrganizationId {
        Objects.requireNonNull(value, "PartnerOrganizationId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PartnerOrganizationId cannot be blank");
        }
    }

    public static PartnerOrganizationId generate() {
        return new PartnerOrganizationId("PARTNER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

