package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.platform.IndustryPolicyJpaEntity;
import com.supplygraph.adapter.persistence.repository.platform.SpringDataIndustryPolicyRepository;
import com.supplygraph.application.platform.port.out.IndustryPolicyRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.IndustryPolicyId;
import com.supplygraph.domain.model.platform.IndustryPolicyRecord;
import com.supplygraph.domain.model.platform.PolicyStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresIndustryPolicyPersistenceAdapter implements IndustryPolicyRepositoryPort {

    private final SpringDataIndustryPolicyRepository repository;

    public PostgresIndustryPolicyPersistenceAdapter(SpringDataIndustryPolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(IndustryPolicyRecord record) {
        IndustryPolicyJpaEntity entity = new IndustryPolicyJpaEntity(
                record.id().value(),
                record.workspaceId().value(),
                record.industryCode(),
                record.versionNumber(),
                record.policyJson(),
                record.status().name(),
                record.createdAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<IndustryPolicyRecord> findById(IndustryPolicyId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<IndustryPolicyRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<IndustryPolicyRecord> findByWorkspaceIdAndIndustryCode(WorkspaceId workspaceId, String industryCode) {
        return repository.findByWorkspaceIdAndIndustryCode(workspaceId.value(), industryCode).stream()
                .map(this::toDomain)
                .toList();
    }

    private IndustryPolicyRecord toDomain(IndustryPolicyJpaEntity entity) {
        return new IndustryPolicyRecord(
                new IndustryPolicyId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getIndustryCode(),
                entity.getVersionNumber(),
                entity.getPolicyJson(),
                PolicyStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt()
        );
    }
}

