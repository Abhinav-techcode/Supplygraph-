package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.PlanningScenarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPlanningScenarioRepository extends JpaRepository<PlanningScenarioJpaEntity, String> {
    List<PlanningScenarioJpaEntity> findByWorkspaceId(String workspaceId);
}

