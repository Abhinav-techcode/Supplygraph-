package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.intelligence.MaterialIntelligenceProfile;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for material criticality, runway calculations, and inventory balances.
 */
public interface MaterialIntelligenceRepositoryPort {

    void saveAll(String workspaceId, List<MaterialIntelligenceProfile> profiles);

    List<MaterialIntelligenceProfile> findByWorkspaceId(String workspaceId);

    Optional<MaterialIntelligenceProfile> findByMaterialId(String workspaceId, String materialId);
}
