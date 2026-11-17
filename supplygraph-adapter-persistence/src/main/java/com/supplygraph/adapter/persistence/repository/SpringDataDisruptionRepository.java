package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.DisruptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataDisruptionRepository extends JpaRepository<DisruptionJpaEntity, String> {
    List<DisruptionJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<DisruptionJpaEntity> findByWorkspaceIdAndStatus(String workspaceId, String status);
    long countByWorkspaceIdAndStatus(String workspaceId, String status);
}

