package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.MonitoringRuleJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataMonitoringRuleRepository;
import com.supplygraph.application.port.out.MonitoringRuleRepositoryPort;
import com.supplygraph.domain.model.monitoring.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresMonitoringRuleRepositoryAdapter implements MonitoringRuleRepositoryPort {

    private final SpringDataMonitoringRuleRepository repository;

    public PostgresMonitoringRuleRepositoryAdapter(SpringDataMonitoringRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(MonitoringRule rule) {
        MonitoringRuleJpaEntity entity = new MonitoringRuleJpaEntity(
                rule.getId().value(),
                rule.getName(),
                rule.getDescription(),
                rule.getIndustryApplicability(),
                rule.getSeverity().name(),
                rule.getThreshold(),
                rule.getTrigger().name(),
                rule.getConditionType(),
                rule.isEnabled()
        );
        repository.save(entity);
    }

    @Override
    public Optional<MonitoringRule> findById(MonitoringRuleId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<MonitoringRule> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<MonitoringRule> findByIndustry(String industry) {
        return repository.findByIndustryApplicabilityOrIndustryApplicability(industry, "ALL")
                .stream().map(this::toDomain).toList();
    }

    private MonitoringRule toDomain(MonitoringRuleJpaEntity entity) {
        return new MonitoringRule(
                MonitoringRuleId.of(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getIndustryApplicability(),
                MonitoringSeverity.valueOf(entity.getSeverity()),
                entity.getThreshold(),
                MonitoringEvaluationTrigger.valueOf(entity.getTriggerType()),
                entity.getConditionType(),
                entity.isEnabled()
        );
    }
}

