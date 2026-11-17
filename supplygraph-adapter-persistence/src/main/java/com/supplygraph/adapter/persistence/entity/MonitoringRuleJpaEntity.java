package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "monitoring_rules")
public class MonitoringRuleJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "industry_applicability", length = 64, nullable = false)
    private String industryApplicability;

    @Column(name = "severity", length = 32, nullable = false)
    private String severity;

    @Column(name = "threshold", precision = 19, scale = 4, nullable = false)
    private BigDecimal threshold;

    @Column(name = "trigger_type", length = 32, nullable = false)
    private String triggerType;

    @Column(name = "condition_type", length = 64, nullable = false)
    private String conditionType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected MonitoringRuleJpaEntity() {}

    public MonitoringRuleJpaEntity(
            String id, String name, String description, String industryApplicability,
            String severity, BigDecimal threshold, String triggerType, String conditionType, boolean enabled
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.industryApplicability = industryApplicability;
        this.severity = severity;
        this.threshold = threshold;
        this.triggerType = triggerType;
        this.conditionType = conditionType;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIndustryApplicability() { return industryApplicability; }
    public String getSeverity() { return severity; }
    public BigDecimal getThreshold() { return threshold; }
    public String getTriggerType() { return triggerType; }
    public String getConditionType() { return conditionType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

