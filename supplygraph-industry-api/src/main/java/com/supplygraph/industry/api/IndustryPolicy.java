package com.supplygraph.industry.api;

import java.util.Objects;

/**
 * Deterministic constraint policy supplied by an IndustryPackExtension.
 */
public record IndustryPolicy(
        String policyCode,
        String name,
        String description,
        boolean requiresHumanApproval
) {
    public IndustryPolicy {
        Objects.requireNonNull(policyCode, "policyCode must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
