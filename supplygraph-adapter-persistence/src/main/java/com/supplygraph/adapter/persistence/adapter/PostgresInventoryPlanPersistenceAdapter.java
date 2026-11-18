package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.InventoryPlanJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataInventoryPlanRepository;
import com.supplygraph.application.planning.port.out.InventoryPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.EchelonInventoryPosition;
import com.supplygraph.domain.model.planning.InventoryPlan;
import com.supplygraph.domain.model.planning.InventoryPlanId;
import com.supplygraph.domain.model.planning.ServiceLevel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresInventoryPlanPersistenceAdapter implements InventoryPlanRepositoryPort {

    private final SpringDataInventoryPlanRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresInventoryPlanPersistenceAdapter(SpringDataInventoryPlanRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public void save(InventoryPlan plan) {
        try {
            String positionsJson = objectMapper.writeValueAsString(plan.positions());

            InventoryPlanJpaEntity entity = new InventoryPlanJpaEntity(
                    plan.id().value(),
                    plan.workspaceId().value(),
                    plan.planName(),
                    plan.targetServiceLevel().name(),
                    positionsJson,
                    plan.totalCarryingCost(),
                    plan.calculatedAt()
            );

            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist inventory plan: " + plan.id().value(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryPlan> findById(InventoryPlanId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryPlan> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private InventoryPlan toDomain(InventoryPlanJpaEntity entity) {
        try {
            List<EchelonInventoryPosition> positions = objectMapper.readValue(entity.getPositionsJson(), new TypeReference<>() {});

            return new InventoryPlan(
                    new InventoryPlanId(entity.getId()),
                    new WorkspaceId(entity.getWorkspaceId()),
                    entity.getPlanName(),
                    ServiceLevel.valueOf(entity.getTargetServiceLevel()),
                    positions,
                    entity.getTotalCarryingCost(),
                    entity.getCalculatedAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize inventory plan entity: " + entity.getId(), e);
        }
    }
}
