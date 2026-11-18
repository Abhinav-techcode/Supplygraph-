package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ProcurementPlanJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataProcurementPlanRepository;
import com.supplygraph.application.planning.port.out.ProcurementPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.NetRequirementItem;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.domain.model.planning.ProcurementPlanId;
import com.supplygraph.domain.model.planning.SupplierAllocationShare;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresProcurementPlanPersistenceAdapter implements ProcurementPlanRepositoryPort {

    private final SpringDataProcurementPlanRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresProcurementPlanPersistenceAdapter(SpringDataProcurementPlanRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public void save(ProcurementPlan plan) {
        try {
            String netReqsJson = objectMapper.writeValueAsString(plan.netRequirements());
            String allocationsJson = objectMapper.writeValueAsString(plan.supplierAllocations());

            ProcurementPlanJpaEntity entity = new ProcurementPlanJpaEntity(
                    plan.id().value(),
                    plan.workspaceId().value(),
                    plan.planName(),
                    netReqsJson,
                    allocationsJson,
                    plan.totalProjectedCost(),
                    plan.generatedAt()
            );

            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist procurement plan: " + plan.id().value(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcurementPlan> findById(ProcurementPlanId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcurementPlan> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private ProcurementPlan toDomain(ProcurementPlanJpaEntity entity) {
        try {
            List<NetRequirementItem> netReqs = objectMapper.readValue(entity.getNetRequirementsJson(), new TypeReference<>() {});
            List<SupplierAllocationShare> allocations = objectMapper.readValue(entity.getSupplierAllocationsJson(), new TypeReference<>() {});

            return new ProcurementPlan(
                    new ProcurementPlanId(entity.getId()),
                    new WorkspaceId(entity.getWorkspaceId()),
                    entity.getPlanName(),
                    netReqs,
                    allocations,
                    entity.getTotalProjectedCost(),
                    entity.getGeneratedAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize procurement plan entity: " + entity.getId(), e);
        }
    }
}
