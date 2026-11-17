package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.MonteCarloRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataMonteCarloRunRepository extends JpaRepository<MonteCarloRunJpaEntity, String> {
    List<MonteCarloRunJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<MonteCarloRunJpaEntity> findByDisruptionIdOrderByCreatedAtDesc(String disruptionId);
    long countByWorkspaceId(String workspaceId);
}

