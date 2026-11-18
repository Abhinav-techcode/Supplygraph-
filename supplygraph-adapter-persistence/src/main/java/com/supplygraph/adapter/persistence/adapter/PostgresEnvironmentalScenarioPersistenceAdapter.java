package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.EnvironmentalScenarioJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataEnvironmentalScenarioRepository;
import com.supplygraph.application.sustainability.port.out.EnvironmentalScenarioRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenario;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenarioId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresEnvironmentalScenarioPersistenceAdapter implements EnvironmentalScenarioRepositoryPort {

    private final SpringDataEnvironmentalScenarioRepository repository;

    public PostgresEnvironmentalScenarioPersistenceAdapter(SpringDataEnvironmentalScenarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(EnvironmentalScenario scenario) {
        EnvironmentalScenarioJpaEntity entity = new EnvironmentalScenarioJpaEntity(
                scenario.id().value(),
                scenario.workspaceId().value(),
                scenario.scenarioName(),
                scenario.baselineCo2Tons(),
                scenario.projectedCo2Tons(),
                scenario.reductionPercentage(),
                scenario.totalAbatementCostUsd(),
                scenario.costPerTonCo2AvoidedUsd(),
                scenario.evaluatedAt()
        );

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnvironmentalScenario> findById(EnvironmentalScenarioId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalScenario> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private EnvironmentalScenario toDomain(EnvironmentalScenarioJpaEntity entity) {
        return new EnvironmentalScenario(
                new EnvironmentalScenarioId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getScenarioName(),
                entity.getBaselineCo2Tons(),
                entity.getProjectedCo2Tons(),
                entity.getReductionPercentage(),
                entity.getTotalAbatementCostUsd(),
                entity.getCostPerTonCo2AvoidedUsd(),
                entity.getEvaluatedAt()
        );
    }
}

