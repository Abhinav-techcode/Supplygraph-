package com.supplygraph.domain.model.explainability;

import java.io.Serializable;
import java.util.Objects;

/**
 * Operational or policy constraint evaluated during recovery recommendation generation.
 */
public record DecisionConstraint(
        String code,
        String description,
        ConstraintType type,
        boolean satisfied,
        String violationImpact
) implements Serializable {

    public enum ConstraintType {
        HARD,
        SOFT,
        REGULATORY_POLICY
    }

    public DecisionConstraint {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    public static DecisionConstraint of(String code, String description, ConstraintType type, boolean satisfied, String violationImpact) {
        return new DecisionConstraint(code, description, type, satisfied, violationImpact);
    }
}

