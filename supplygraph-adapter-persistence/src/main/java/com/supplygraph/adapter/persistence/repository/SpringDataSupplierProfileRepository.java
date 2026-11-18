package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.SupplierRiskProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSupplierProfileRepository extends JpaRepository<SupplierRiskProfileJpaEntity, String> {
    List<SupplierRiskProfileJpaEntity> findByWorkspaceId(String workspaceId);
    Optional<SupplierRiskProfileJpaEntity> findByWorkspaceIdAndSupplierId(String workspaceId, String supplierId);
}

