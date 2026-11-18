package com.supplygraph.domain.model.platform;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record IndustryPolicyId(String value) implements Serializable {
    public IndustryPolicyId {
        Objects.requireNonNull(value, "IndustryPolicyId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IndustryPolicyId cannot be blank");
        }
    }

    public static IndustryPolicyId generate() {
        return new IndustryPolicyId("POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

