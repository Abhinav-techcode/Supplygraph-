package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.RiskAlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRiskAlertRepository extends JpaRepository<RiskAlertJpaEntity, String> {
    List<RiskAlertJpaEntity> findByWorkspaceIdOrderByDetectedAtDesc(String workspaceId);
    List<RiskAlertJpaEntity> findByWorkspaceIdAndStatusOrderByDetectedAtDesc(String workspaceId, String status);
    Optional<RiskAlertJpaEntity> findBySuppressionKeyAndStatus(String suppressionKey, String status);
}

