package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.collaboration.PartnerDocumentJpaEntity;
import com.supplygraph.adapter.persistence.repository.collaboration.SpringDataPartnerDocumentRepository;
import com.supplygraph.application.collaboration.port.out.PartnerDocumentRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.DocumentCategory;
import com.supplygraph.domain.model.collaboration.PartnerDocumentId;
import com.supplygraph.domain.model.collaboration.PartnerDocumentRecord;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresPartnerDocumentPersistenceAdapter implements PartnerDocumentRepositoryPort {

    private final SpringDataPartnerDocumentRepository repository;

    public PostgresPartnerDocumentPersistenceAdapter(SpringDataPartnerDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(PartnerDocumentRecord document) {
        PartnerDocumentJpaEntity entity = new PartnerDocumentJpaEntity(
                document.id().value(),
                document.workspaceId().value(),
                document.partnerId().value(),
                document.fileName(),
                document.category().name(),
                document.cryptographicHash(),
                document.verified(),
                document.uploadedAt()
        );
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PartnerDocumentRecord> findById(PartnerDocumentId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerDocumentRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerDocumentRecord> findByPartnerId(WorkspaceId workspaceId, PartnerOrganizationId partnerId) {
        return repository.findByWorkspaceIdAndPartnerId(workspaceId.value(), partnerId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private PartnerDocumentRecord toDomain(PartnerDocumentJpaEntity e) {
        return new PartnerDocumentRecord(
                new PartnerDocumentId(e.getId()),
                new WorkspaceId(e.getWorkspaceId()),
                new PartnerOrganizationId(e.getPartnerId()),
                e.getFileName(),
                DocumentCategory.valueOf(e.getCategory()),
                e.getCryptographicHash(),
                e.isVerified(),
                e.getUploadedAt()
        );
    }
}

