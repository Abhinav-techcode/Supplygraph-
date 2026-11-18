package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.intelligence.SupplierRiskProfile;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for calculated supplier intelligence profiles and operational risk metrics.
 */
public interface SupplierIntelligenceRepositoryPort {

    void saveAll(String workspaceId, List<SupplierRiskProfile> profiles);

    List<SupplierRiskProfile> findByWorkspaceId(String workspaceId);

    Optional<SupplierRiskProfile> findBySupplierId(String workspaceId, String supplierId);
}

