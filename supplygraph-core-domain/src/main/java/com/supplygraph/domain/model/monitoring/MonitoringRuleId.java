package com.supplygraph.domain.model.monitoring;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a continuous monitoring rule.
 */
public record MonitoringRuleId(String value) {
    public MonitoringRuleId {
        Objects.requireNonNull(value, "MonitoringRuleId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MonitoringRuleId value must not be blank");
        }
    }

    public static MonitoringRuleId of(String value) {
        return new MonitoringRuleId(value);
    }

    public static MonitoringRuleId generate() {
        return new MonitoringRuleId("RULE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

