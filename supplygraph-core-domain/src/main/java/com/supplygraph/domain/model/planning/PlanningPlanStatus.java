package com.supplygraph.domain.model.planning;

/**
 * Governed lifecycle stages of a strategic supply chain plan requiring human authorization.
 */
public enum PlanningPlanStatus {
    DRAFT("Draft Formulation"),
    EVALUATED("Optimized & Evaluated"),
    PENDING_APPROVAL("Pending Human Sign-off"),
    APPROVED("Approved & Released to Execution"),
    REJECTED("Rejected by Governance Authority");

    private final String description;

    PlanningPlanStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

