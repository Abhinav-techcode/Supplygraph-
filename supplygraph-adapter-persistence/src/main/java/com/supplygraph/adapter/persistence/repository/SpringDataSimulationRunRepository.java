package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.SimulationRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSimulationRunRepository extends JpaRepository<SimulationRunJpaEntity, String> {
    List<SimulationRunJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<SimulationRunJpaEntity> findByDisruptionIdOrderByCreatedAtDesc(String disruptionId);
    long countByWorkspaceId(String workspaceId);
}

