package com.supplygraph.domain.model.sustainability;

/**
 * Three pillars of ESG compliance and supplier sustainability.
 */
public enum EsgPillar {
    ENVIRONMENTAL("Environmental Stewardship & Decarbonization"),
    SOCIAL("Labor Practices, Human Rights & Fair Sourcing"),
    GOVERNANCE("Business Ethics, Anti-Corruption & Compliance");

    private final String description;

    EsgPillar(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

