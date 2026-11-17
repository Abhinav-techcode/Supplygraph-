package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.monitoring.MonitoringRule;
import com.supplygraph.domain.model.monitoring.MonitoringRuleId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for monitoring rules.
 */
public interface MonitoringRuleRepositoryPort {
    void save(MonitoringRule rule);
    Optional<MonitoringRule> findById(MonitoringRuleId id);
    List<MonitoringRule> findAll();
    List<MonitoringRule> findByIndustry(String industry);
}

