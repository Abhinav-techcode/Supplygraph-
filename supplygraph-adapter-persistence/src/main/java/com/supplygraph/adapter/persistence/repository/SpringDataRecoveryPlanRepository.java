package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.RecoveryPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataRecoveryPlanRepository extends JpaRepository<RecoveryPlanJpaEntity, String> {
    List<RecoveryPlanJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<RecoveryPlanJpaEntity> findByDisruptionIdOrderByCreatedAtDesc(String disruptionId);
    long countByWorkspaceIdAndStatus(String workspaceId, String status);
}

