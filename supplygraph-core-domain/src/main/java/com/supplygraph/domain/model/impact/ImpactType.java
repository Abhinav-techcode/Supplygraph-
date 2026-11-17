package com.supplygraph.domain.model.impact;

/**
 * Distinguishes direct versus cascading, confirmed, or potential impacts.
 */
public enum ImpactType {
    DIRECT,
    INDIRECT,
    POTENTIAL,
    CONFIRMED,
    UNKNOWN
}

