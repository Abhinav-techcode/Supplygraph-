package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.NetworkResilienceAssessmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataResilienceRepository extends JpaRepository<NetworkResilienceAssessmentJpaEntity, String> {
    Optional<NetworkResilienceAssessmentJpaEntity> findFirstByWorkspaceIdOrderByAssessedAtDesc(String workspaceId);
    List<NetworkResilienceAssessmentJpaEntity> findByWorkspaceIdOrderByAssessedAtDesc(String workspaceId);
}

