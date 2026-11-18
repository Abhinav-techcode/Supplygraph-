package com.supplygraph.domain.model.whatif;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for an isolated What-If scenario.
 */
public record WhatIfScenarioId(String value) implements Serializable {
    public WhatIfScenarioId {
        Objects.requireNonNull(value, "WhatIfScenarioId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WhatIfScenarioId must not be blank");
        }
    }

    public static WhatIfScenarioId of(String value) {
        return new WhatIfScenarioId(value);
    }

    public static WhatIfScenarioId generate() {
        return new WhatIfScenarioId("WIF-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

