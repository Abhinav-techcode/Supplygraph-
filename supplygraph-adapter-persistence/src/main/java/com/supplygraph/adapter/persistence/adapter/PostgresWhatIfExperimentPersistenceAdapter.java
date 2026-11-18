package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.WhatIfExperimentJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataWhatIfExperimentRepository;
import com.supplygraph.application.port.out.WhatIfExperimentRepositoryPort;
import com.supplygraph.domain.model.whatif.WhatIfComparisonResult;
import com.supplygraph.domain.model.whatif.WhatIfScenarioId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresWhatIfExperimentPersistenceAdapter implements WhatIfExperimentRepositoryPort {

    private final SpringDataWhatIfExperimentRepository repository;

    public PostgresWhatIfExperimentPersistenceAdapter(SpringDataWhatIfExperimentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(WhatIfComparisonResult result) {
        String id = "WIF-EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        WhatIfExperimentJpaEntity entity = new WhatIfExperimentJpaEntity(
                id,
                result.scenarioId().value(),
                result.scenarioName(),
                result.baselineCostUsd(),
                result.scenarioCostUsd(),
                result.baselineTimeToRecoverDays(),
                result.scenarioTimeToRecoverDays(),
                result.baselineServiceLevelPct(),
                result.scenarioServiceLevelPct(),
                result.baselineResilienceIndex(),
                result.scenarioResilienceIndex(),
                result.costVariancePct(),
                result.recoveryTimeDeltaDays(),
                result.varianceSummary(),
                result.evaluatedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<WhatIfComparisonResult> findByScenarioId(WhatIfScenarioId scenarioId) {
        return repository.findTopByScenarioIdOrderByEvaluatedAtDesc(scenarioId.value()).map(this::toDomain);
    }

    @Override
    public List<WhatIfComparisonResult> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private WhatIfComparisonResult toDomain(WhatIfExperimentJpaEntity e) {
        return WhatIfComparisonResult.of(
                WhatIfScenarioId.of(e.getScenarioId()),
                e.getScenarioName(),
                e.getBaselineCostUsd(),
                e.getScenarioCostUsd(),
                e.getBaselineTtrDays(),
                e.getScenarioTtrDays(),
                e.getBaselineServicePct(),
                e.getScenarioServicePct(),
                e.getBaselineResilienceIdx(),
                e.getScenarioResilienceIdx(),
                e.getCostVariancePct(),
                e.getRecoveryTimeDeltaDays(),
                e.getVarianceSummary(),
                e.getEvaluatedAt()
        );
    }
}

