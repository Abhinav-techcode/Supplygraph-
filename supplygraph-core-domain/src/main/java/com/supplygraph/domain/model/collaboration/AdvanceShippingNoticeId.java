package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record AdvanceShippingNoticeId(String value) implements Serializable {
    public AdvanceShippingNoticeId {
        Objects.requireNonNull(value, "AdvanceShippingNoticeId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("AdvanceShippingNoticeId cannot be blank");
        }
    }

    public static AdvanceShippingNoticeId generate() {
        return new AdvanceShippingNoticeId("ASN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

