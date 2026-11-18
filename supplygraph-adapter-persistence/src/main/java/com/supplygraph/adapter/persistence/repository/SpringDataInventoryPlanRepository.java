package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.InventoryPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataInventoryPlanRepository extends JpaRepository<InventoryPlanJpaEntity, String> {
    List<InventoryPlanJpaEntity> findByWorkspaceId(String workspaceId);
}

