package com.supplygraph.domain.model.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable provenance record tracking the source, origin entity, and transmission context of an operational event.
 */
public record EventProvenance(
        String sourceSystem,
        String sourceUser,
        String importBatchId,
        String certificateOrToken,
        Instant recordedAt
) {
    public EventProvenance {
        Objects.requireNonNull(sourceSystem, "sourceSystem must not be null");
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }

    public static EventProvenance internal(String user) {
        return new EventProvenance("SUPPLYGRAPH_INTERNAL", user != null ? user : "SYSTEM", null, null, Instant.now());
    }

    public static EventProvenance imported(String system, String batchId, String user) {
        return new EventProvenance(system, user, batchId, null, Instant.now());
    }

    public static EventProvenance synthetic(String scenarioName) {
        return new EventProvenance("SYNTHETIC_TEST_RUNNER", "SCENARIO_" + scenarioName, null, null, Instant.now());
    }
}

