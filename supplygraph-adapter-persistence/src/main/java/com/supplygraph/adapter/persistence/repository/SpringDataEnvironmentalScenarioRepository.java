package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.EnvironmentalScenarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataEnvironmentalScenarioRepository extends JpaRepository<EnvironmentalScenarioJpaEntity, String> {
    List<EnvironmentalScenarioJpaEntity> findByWorkspaceId(String workspaceId);
}

