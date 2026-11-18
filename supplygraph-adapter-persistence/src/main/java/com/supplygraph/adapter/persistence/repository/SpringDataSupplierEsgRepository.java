package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.SupplierEsgScorecardJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSupplierEsgRepository extends JpaRepository<SupplierEsgScorecardJpaEntity, String> {
    List<SupplierEsgScorecardJpaEntity> findByWorkspaceId(String workspaceId);
    Optional<SupplierEsgScorecardJpaEntity> findByWorkspaceIdAndSupplierId(String workspaceId, String supplierId);
}

