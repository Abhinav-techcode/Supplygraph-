package com.supplygraph.domain.model.explainability;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Empirical evidence supporting an explainable recommendation.
 */
public record DecisionEvidence(
        String id,
        String title,
        String source,
        double confidenceScore,
        Instant timestamp,
        String summary
) implements Serializable {
    /** Confidence value meaning "no measurement exists"; never shown as a percentage. */
    public static final double NOT_MEASURED = -1.0;

    public DecisionEvidence {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static DecisionEvidence of(String id, String title, String source, double confidenceScore, Instant timestamp, String summary) {
        return new DecisionEvidence(id, title, source, confidenceScore, timestamp, summary);
    }
}

