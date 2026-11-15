package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.InventoryBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataInventoryBalanceRepository extends JpaRepository<InventoryBalanceJpaEntity, String> {
    Optional<InventoryBalanceJpaEntity> findByWorkspaceIdAndNodeIdAndMaterialIdAndCategory(
            String workspaceId, String nodeId, String materialId, String category);
}

