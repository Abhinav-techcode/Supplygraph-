package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.CircularLedgerJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataCircularLedgerRepository;
import com.supplygraph.application.sustainability.port.out.CircularLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.sustainability.CircularFlowType;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntryId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresCircularLedgerPersistenceAdapter implements CircularLedgerRepositoryPort {

    private final SpringDataCircularLedgerRepository repository;

    public PostgresCircularLedgerPersistenceAdapter(SpringDataCircularLedgerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(CircularLedgerEntry entry) {
        CircularLedgerJpaEntity entity = new CircularLedgerJpaEntity(
                entry.id().value(),
                entry.workspaceId().value(),
                entry.materialId().value(),
                entry.flowType().name(),
                entry.intakeQuantity(),
                entry.recoveryYieldPercentage(),
                entry.recoveredQuantity(),
                entry.virginMaterialAvoidedKg(),
                entry.recordedAt()
        );

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CircularLedgerEntry> findById(CircularLedgerEntryId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircularLedgerEntry> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private CircularLedgerEntry toDomain(CircularLedgerJpaEntity entity) {
        return new CircularLedgerEntry(
                new CircularLedgerEntryId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                new MaterialId(entity.getMaterialId()),
                CircularFlowType.valueOf(entity.getFlowType()),
                entity.getIntakeQuantity(),
                entity.getRecoveryYieldPercentage(),
                entity.getRecoveredQuantity(),
                entity.getVirginMaterialAvoidedKg(),
                entity.getRecordedAt()
        );
    }
}

