package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.collaboration.QualityIncident8DJpaEntity;
import com.supplygraph.adapter.persistence.repository.collaboration.SpringDataSupplierQualityRepository;
import com.supplygraph.application.collaboration.port.out.SupplierQualityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.EightDStep;
import com.supplygraph.domain.model.collaboration.IncidentSeverity;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.collaboration.QualityIncident8DId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresSupplierQualityPersistenceAdapter implements SupplierQualityRepositoryPort {

    private final SpringDataSupplierQualityRepository repository;

    public PostgresSupplierQualityPersistenceAdapter(SpringDataSupplierQualityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(QualityIncident8D incident) {
        QualityIncident8DJpaEntity entity = new QualityIncident8DJpaEntity(
                incident.id().value(),
                incident.workspaceId().value(),
                incident.supplierId().value(),
                incident.materialId().value(),
                incident.defectTitle(),
                incident.severity().name(),
                incident.currentStep().name(),
                incident.closed(),
                incident.reportedAt()
        );
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QualityIncident8D> findById(QualityIncident8DId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityIncident8D> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private QualityIncident8D toDomain(QualityIncident8DJpaEntity e) {
        return new QualityIncident8D(
                new QualityIncident8DId(e.getId()),
                new WorkspaceId(e.getWorkspaceId()),
                new PartnerOrganizationId(e.getSupplierId()),
                new MaterialId(e.getMaterialId()),
                e.getDefectTitle(),
                IncidentSeverity.valueOf(e.getSeverity()),
                EightDStep.valueOf(e.getCurrentStep()),
                e.isClosed(),
                e.getReportedAt()
        );
    }
}

