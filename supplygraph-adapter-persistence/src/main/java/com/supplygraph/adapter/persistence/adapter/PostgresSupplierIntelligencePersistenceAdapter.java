package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.SupplierRiskProfileJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataSupplierProfileRepository;
import com.supplygraph.application.port.out.SupplierIntelligenceRepositoryPort;
import com.supplygraph.domain.model.intelligence.SupplierRiskProfile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresSupplierIntelligencePersistenceAdapter implements SupplierIntelligenceRepositoryPort {

    private final SpringDataSupplierProfileRepository repository;

    public PostgresSupplierIntelligencePersistenceAdapter(SpringDataSupplierProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveAll(String workspaceId, List<SupplierRiskProfile> profiles) {
        for (SupplierRiskProfile profile : profiles) {
            String compositeId = workspaceId + ":" + profile.supplierId();
            SupplierRiskProfileJpaEntity entity = repository.findById(compositeId).orElseGet(SupplierRiskProfileJpaEntity::new);
            entity.setId(compositeId);
            entity.setWorkspaceId(workspaceId);
            entity.setSupplierId(profile.supplierId());
            entity.setSupplierName(profile.supplierName());
            entity.setTier(profile.tier());
            entity.setCountry(profile.country());
            entity.setOverallRiskScore(BigDecimal.valueOf(profile.overallRiskScore()));
            entity.setSingleSource(profile.isSingleSourceForCriticalMaterials());
            entity.setDownstreamSitesImpacted(profile.downstreamSitesImpacted());
            entity.setAlternativeSuppliersCount(profile.alternativeSuppliersCount());
            entity.setQualificationStatus(profile.qualificationStatus());
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierRiskProfile> findByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceId(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupplierRiskProfile> findBySupplierId(String workspaceId, String supplierId) {
        return repository.findByWorkspaceIdAndSupplierId(workspaceId, supplierId).map(this::toDomain);
    }

    private SupplierRiskProfile toDomain(SupplierRiskProfileJpaEntity entity) {
        return new SupplierRiskProfile(
                entity.getSupplierId(),
                entity.getSupplierName(),
                entity.getTier(),
                entity.getCountry(),
                entity.getOverallRiskScore().doubleValue(),
                new ArrayList<>(), // Detailed metrics loaded on demand
                entity.isSingleSource(),
                entity.getDownstreamSitesImpacted(),
                entity.getAlternativeSuppliersCount(),
                entity.getQualificationStatus()
        );
    }
}
