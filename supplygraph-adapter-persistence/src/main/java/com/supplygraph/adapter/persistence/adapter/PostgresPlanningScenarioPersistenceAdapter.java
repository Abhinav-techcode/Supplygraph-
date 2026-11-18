package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.PlanningScenarioJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataPlanningScenarioRepository;
import com.supplygraph.application.planning.port.out.PlanningScenarioRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.planning.PlanningScenarioId;
import com.supplygraph.domain.model.planning.PlanningScenarioType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresPlanningScenarioPersistenceAdapter implements PlanningScenarioRepositoryPort {

    private final SpringDataPlanningScenarioRepository repository;

    public PostgresPlanningScenarioPersistenceAdapter(SpringDataPlanningScenarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(PlanningScenarioComparison comparison) {
        PlanningScenarioJpaEntity entity = new PlanningScenarioJpaEntity(
                comparison.scenarioId().value(),
                comparison.workspaceId().value(),
                comparison.scenarioName(),
                comparison.scenarioType().name(),
                comparison.status().name(),
                comparison.totalProjectedCost(),
                comparison.customerServiceFillRate(),
                comparison.averageLeadTimeDays(),
                comparison.carbonFootprintCo2Tons(),
                comparison.evaluatedBy(),
                comparison.evaluatedAt()
        );

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanningScenarioComparison> findById(PlanningScenarioId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanningScenarioComparison> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private PlanningScenarioComparison toDomain(PlanningScenarioJpaEntity entity) {
        return new PlanningScenarioComparison(
                new PlanningScenarioId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getScenarioName(),
                PlanningScenarioType.valueOf(entity.getScenarioType()),
                PlanningPlanStatus.valueOf(entity.getStatus()),
                entity.getTotalProjectedCost(),
                entity.getCustomerServiceFillRate(),
                entity.getAverageLeadTimeDays(),
                entity.getCarbonFootprintCo2Tons(),
                entity.getEvaluatedBy(),
                entity.getEvaluatedAt()
        );
    }
}

