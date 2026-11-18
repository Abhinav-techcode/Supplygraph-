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
    DATA_IMPORTED("Enterprise dataset batch imported and validated"),
    SECURITY_POLICY_ENFORCED("Security constraint or rate limit enforced"),
    CONNECTOR_REGISTERED("Enterprise connector registered"),
    CONNECTOR_CONFIGURED("Enterprise connector configuration changed"),
    CONNECTOR_ACTIVATED("Enterprise connector activated"),
    CONNECTOR_DEACTIVATED("Enterprise connector deactivated"),
    EVENT_REPLAYED("Dead-lettered integration event replayed"),
    CONNECTOR_EXECUTED("Connector operation executed"),
    CONNECTOR_EXECUTION_RETRIED("Failed connector execution retried"),
    WEBHOOK_REJECTED("Inbound webhook rejected"),
    RECONCILIATION_RESOLVED("Synchronization conflict resolved by a human"),
    FIELD_POLICY_CHANGED("Field authority policy changed"),
    WORKFLOW_CREATED("Execution workflow created from approved plan"),
    WORKFLOW_BLOCKED("Execution workflow blocked"),
    WORKFLOW_APPROVED("Execution workflow approved"),
    WORKFLOW_REJECTED("Execution workflow rejected"),
    WORKFLOW_STARTED("Execution workflow execution started"),
    WORKFLOW_CANCELLED("Execution workflow cancelled"),
    TASK_RETRIED("Execution task retried"),
    TASK_RECONCILED("Ambiguous execution outcome reconciled"),
    TASK_COMPENSATED("Execution task compensated"),
    REASSESSMENT_PROPOSED("Reassessment proposed after plan deviation"),
    REASSESSMENT_DECIDED("Reassessment proposal decided by a human");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

