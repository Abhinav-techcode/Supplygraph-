package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.AdvancedResilienceMetricsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataAdvancedResilienceRepository extends JpaRepository<AdvancedResilienceMetricsJpaEntity, String> {
    Optional<AdvancedResilienceMetricsJpaEntity> findTopByWorkspaceIdOrderByCalculatedAtDesc(String workspaceId);
}

