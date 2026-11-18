package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.collaboration.PartnerOrganizationJpaEntity;
import com.supplygraph.adapter.persistence.repository.collaboration.SpringDataPartnerOrganizationRepository;
import com.supplygraph.application.collaboration.port.out.PartnerOrganizationRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.PartnerTier;
import com.supplygraph.domain.model.collaboration.PartnerType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresPartnerOrganizationPersistenceAdapter implements PartnerOrganizationRepositoryPort {

    private final SpringDataPartnerOrganizationRepository repository;

    public PostgresPartnerOrganizationPersistenceAdapter(SpringDataPartnerOrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(PartnerOrganization organization) {
        PartnerOrganizationJpaEntity entity = new PartnerOrganizationJpaEntity(
                organization.id().value(),
                organization.workspaceId().value(),
                organization.name(),
                organization.type().name(),
                organization.tier().name(),
                organization.dunsNumber(),
                organization.countryCode(),
                organization.active(),
                organization.onboardedAt()
        );
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PartnerOrganization> findById(PartnerOrganizationId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerOrganization> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private PartnerOrganization toDomain(PartnerOrganizationJpaEntity e) {
        return new PartnerOrganization(
                new PartnerOrganizationId(e.getId()),
                new WorkspaceId(e.getWorkspaceId()),
                e.getName(),
                PartnerType.valueOf(e.getPartnerType()),
                PartnerTier.valueOf(e.getPartnerTier()),
                e.getDunsNumber(),
                e.getCountryCode(),
                e.isActive(),
                e.getOnboardedAt()
        );
    }
}

