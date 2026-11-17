package com.supplygraph.domain.model.audit;

/**
 * Enumeration of audited lifecycle events and governance operations.
 */
public enum AuditAction {
    PLAN_APPROVED("Human manager approved recovery plan"),
    PLAN_REJECTED("Human manager rejected recovery plan"),
    DISRUPTION_CREATED("Disruption incident registered"),
    DISRUPTION_RESOLVED("Disruption incident marked as resolved"),
    DIGITAL_TWIN_ADVANCED("Digital twin state advanced to next version"),
    REPLANNING_TRIGGERED("Adaptive replanning triggered by continuous monitoring"),
    USER_ROLE_CHANGED("User role or workspace assignment modified"),
    MONITORING_RULE_MODIFIED("Operational monitoring rule created or updated"),
    SECURITY_POLICY_ENFORCED("Security constraint or rate limit enforced");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

