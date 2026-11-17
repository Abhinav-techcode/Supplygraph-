package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.MaterialIntelligenceProfileJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataMaterialProfileRepository;
import com.supplygraph.application.port.out.MaterialIntelligenceRepositoryPort;
import com.supplygraph.domain.model.intelligence.MaterialCriticalityLevel;
import com.supplygraph.domain.model.intelligence.MaterialIntelligenceProfile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresMaterialIntelligencePersistenceAdapter implements MaterialIntelligenceRepositoryPort {

    private final SpringDataMaterialProfileRepository repository;

    public PostgresMaterialIntelligencePersistenceAdapter(SpringDataMaterialProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveAll(String workspaceId, List<MaterialIntelligenceProfile> profiles) {
        for (MaterialIntelligenceProfile profile : profiles) {
            String compositeId = workspaceId + ":" + profile.materialId();
            MaterialIntelligenceProfileJpaEntity entity = repository.findById(compositeId).orElseGet(MaterialIntelligenceProfileJpaEntity::new);
            entity.setId(compositeId);
            entity.setWorkspaceId(workspaceId);
            entity.setMaterialId(profile.materialId());
            entity.setMaterialName(profile.materialName());
            entity.setCategory(profile.category());
            entity.setCriticality(profile.criticality().name());
            entity.setSingleSourceSupplierId(profile.singleSourceSupplierId());
            entity.setLeadTimeDays(profile.leadTimeDays());
            entity.setOnHandQuantity(profile.onHandQuantity());
            entity.setAvailableQuantity(profile.availableQuantity());
            entity.setReservedQuantity(profile.reservedQuantity());
            entity.setQuarantinedQuantity(profile.quarantinedQuantity());
            entity.setInTransitQuantity(profile.inTransitQuantity());
            entity.setDailyConsumptionRate(profile.dailyConsumptionRate());
            entity.setInventoryRunwayDays(profile.inventoryRunwayDays());
            entity.setExpiryRiskBatchCount(profile.expiryRiskBatchCount());
            entity.setDownstreamProductCount(profile.downstreamProductCount());
            entity.setIndustryClassification(profile.industryClassification());
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialIntelligenceProfile> findByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceId(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaterialIntelligenceProfile> findByMaterialId(String workspaceId, String materialId) {
        return repository.findByWorkspaceIdAndMaterialId(workspaceId, materialId).map(this::toDomain);
    }

    private MaterialIntelligenceProfile toDomain(MaterialIntelligenceProfileJpaEntity entity) {
        return new MaterialIntelligenceProfile(
                entity.getMaterialId(),
                entity.getMaterialName(),
                entity.getCategory(),
                MaterialCriticalityLevel.valueOf(entity.getCriticality()),
                entity.getSingleSourceSupplierId(),
                entity.getLeadTimeDays(),
                entity.getOnHandQuantity(),
                entity.getAvailableQuantity(),
                entity.getReservedQuantity(),
                entity.getQuarantinedQuantity(),
                entity.getInTransitQuantity(),
                entity.getDailyConsumptionRate(),
                entity.getInventoryRunwayDays(),
                entity.getExpiryRiskBatchCount(),
                entity.getDownstreamProductCount(),
                entity.getIndustryClassification()
        );
    }
}
