package com.supplygraph.concurguard.arbitration;

import java.util.Objects;
import java.util.UUID;

public record DecisionId(String value) {
    public DecisionId {
        Objects.requireNonNull(value, "DecisionId value must not be null");
    }

    public static DecisionId generate() {
        return new DecisionId(UUID.randomUUID().toString());
    }

    public static DecisionId of(String value) {
        return new DecisionId(value);
    }
}
