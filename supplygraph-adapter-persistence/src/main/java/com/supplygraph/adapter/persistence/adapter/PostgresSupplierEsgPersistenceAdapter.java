package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.SupplierEsgScorecardJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataSupplierEsgRepository;
import com.supplygraph.application.sustainability.port.out.SupplierEsgRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.EsgScorecardId;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresSupplierEsgPersistenceAdapter implements SupplierEsgRepositoryPort {

    private final SpringDataSupplierEsgRepository repository;

    public PostgresSupplierEsgPersistenceAdapter(SpringDataSupplierEsgRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(SupplierEsgScorecard scorecard) {
        SupplierEsgScorecardJpaEntity entity = new SupplierEsgScorecardJpaEntity(
                scorecard.id().value(),
                scorecard.workspaceId().value(),
                scorecard.supplierId().value(),
                scorecard.environmentalScore(),
                scorecard.socialScore(),
                scorecard.governanceScore(),
                scorecard.compositeEsgScore(),
                scorecard.cbamCompliant(),
                scorecard.conflictMineralsCertified(),
                scorecard.eudrCompliant(),
                scorecard.auditedAt()
        );

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupplierEsgScorecard> findById(EsgScorecardId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupplierEsgScorecard> findBySupplierId(WorkspaceId workspaceId, NodeId supplierId) {
        return repository.findByWorkspaceIdAndSupplierId(workspaceId.value(), supplierId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierEsgScorecard> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private SupplierEsgScorecard toDomain(SupplierEsgScorecardJpaEntity entity) {
        return new SupplierEsgScorecard(
                new EsgScorecardId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                new NodeId(entity.getSupplierId()),
                entity.getEnvironmentalScore(),
                entity.getSocialScore(),
                entity.getGovernanceScore(),
                entity.getCompositeEsgScore(),
                entity.isCbamCompliant(),
                entity.isConflictMineralsCertified(),
                entity.isEudrCompliant(),
                entity.getAuditedAt()
        );
    }
}

