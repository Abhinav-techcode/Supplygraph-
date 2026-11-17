package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ImpactAssessmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataImpactAssessmentRepository extends JpaRepository<ImpactAssessmentJpaEntity, String> {
    List<ImpactAssessmentJpaEntity> findByWorkspaceIdOrderByAssessmentTimestampDesc(String workspaceId);
    Optional<ImpactAssessmentJpaEntity> findFirstByDisruptionIdOrderByAssessmentTimestampDesc(String disruptionId);
    long countByWorkspaceId(String workspaceId);
}

