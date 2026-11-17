package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.OptimizationRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataOptimizationRunRepository extends JpaRepository<OptimizationRunJpaEntity, String> {
    List<OptimizationRunJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<OptimizationRunJpaEntity> findByDisruptionIdOrderByCreatedAtDesc(String disruptionId);
    long countByWorkspaceId(String workspaceId);
}

