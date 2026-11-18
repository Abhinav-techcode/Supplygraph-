package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.CarbonEmissionJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataCarbonEmissionRepository;
import com.supplygraph.application.sustainability.port.out.CarbonEmissionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecordId;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresCarbonEmissionPersistenceAdapter implements CarbonEmissionRepositoryPort {

    private final SpringDataCarbonEmissionRepository repository;

    public PostgresCarbonEmissionPersistenceAdapter(SpringDataCarbonEmissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(CarbonEmissionRecord record) {
        CarbonEmissionJpaEntity entity = new CarbonEmissionJpaEntity(
                record.id().value(),
                record.workspaceId().value(),
                record.facilityOrLaneId().value(),
                record.scope().name(),
                record.activityCategory(),
                record.activityAmount(),
                record.unitOfMeasure(),
                record.emissionFactorKgCo2e(),
                record.totalEmissionsKgCo2e(),
                record.recordedAt()
        );

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarbonEmissionRecord> findById(CarbonEmissionRecordId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarbonEmissionRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private CarbonEmissionRecord toDomain(CarbonEmissionJpaEntity entity) {
        return new CarbonEmissionRecord(
                new CarbonEmissionRecordId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                new NodeId(entity.getFacilityOrLaneId()),
                EmissionScope.valueOf(entity.getScope()),
                entity.getActivityCategory(),
                entity.getActivityAmount(),
                entity.getUnitOfMeasure(),
                entity.getEmissionFactorKgCo2e(),
                entity.getTotalEmissionsKgCo2e(),
                entity.getRecordedAt()
        );
    }
}

