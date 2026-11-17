package com.supplygraph.domain.model.monitoring;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Configurable continuous monitoring rule evaluated by the Operational Monitoring Engine.
 */
public class MonitoringRule {

    private final MonitoringRuleId id;
    private final String name;
    private final String description;
    private final String industryApplicability;
    private final MonitoringSeverity severity;
    private final BigDecimal threshold;
    private final MonitoringEvaluationTrigger trigger;
    private final String conditionType;
    private boolean enabled;

    public MonitoringRule(
            MonitoringRuleId id,
            String name,
            String description,
            String industryApplicability,
            MonitoringSeverity severity,
            BigDecimal threshold,
            MonitoringEvaluationTrigger trigger,
            String conditionType,
            boolean enabled
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.industryApplicability = industryApplicability != null ? industryApplicability : "ALL";
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.threshold = threshold != null ? threshold : BigDecimal.ZERO;
        this.trigger = Objects.requireNonNull(trigger, "trigger must not be null");
        this.conditionType = Objects.requireNonNull(conditionType, "conditionType must not be null");
        this.enabled = enabled;
    }

    public static MonitoringRule of(
            String id, String name, String description, String industry,
            MonitoringSeverity severity, BigDecimal threshold,
            MonitoringEvaluationTrigger trigger, String conditionType
    ) {
        return new MonitoringRule(
                MonitoringRuleId.of(id), name, description, industry,
                severity, threshold, trigger, conditionType, true
        );
    }

    public MonitoringRuleId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIndustryApplicability() {
        return industryApplicability;
    }

    public MonitoringSeverity getSeverity() {
        return severity;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public MonitoringEvaluationTrigger getTrigger() {
        return trigger;
    }

    public String getConditionType() {
        return conditionType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

