package com.supplygraph.domain.model.intelligence;

/**
 * Classification of component and raw material criticality to finished product production continuity.
 */
public enum MaterialCriticalityLevel {
    VERY_HIGH("Very High", "Single-source active component, critical mineral, or long-lead wafer with zero buffer", 5),
    HIGH("High", "Specialized component with lead time > 60 days or limited secondary qualification", 4),
    MEDIUM("Medium", "Manufactured component with qualified secondary suppliers available within 30 days", 3),
    LOW("Low", "Standard multi-sourced catalog item with ample market supply", 2),
    NON_CRITICAL("Non-Critical", "Commodity fasteners, generic bulk consumables, and standard packaging", 1);

    private final String displayName;
    private final String description;
    private final int severityRank;

    MaterialCriticalityLevel(String displayName, String description, int severityRank) {
        this.displayName = displayName;
        this.description = description;
        this.severityRank = severityRank;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int severityRank() {
        return severityRank;
    }
}

