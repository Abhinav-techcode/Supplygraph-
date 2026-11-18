package com.supplygraph.domain.model.explainability;

import java.io.Serializable;
import java.util.Objects;

/**
 * Quantified trade-off between competing supply chain objectives for a recommendation.
 */
public record DecisionTradeOff(
        String dimension,
        String optionAValue,
        String optionBValue,
        String description,
        double relativeWeight
) implements Serializable {
    public DecisionTradeOff {
        Objects.requireNonNull(dimension, "dimension must not be null");
        Objects.requireNonNull(optionAValue, "optionAValue must not be null");
        Objects.requireNonNull(optionBValue, "optionBValue must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }

    public static DecisionTradeOff of(String dimension, String optionAValue, String optionBValue, String description, double relativeWeight) {
        return new DecisionTradeOff(dimension, optionAValue, optionBValue, description, relativeWeight);
    }
}

