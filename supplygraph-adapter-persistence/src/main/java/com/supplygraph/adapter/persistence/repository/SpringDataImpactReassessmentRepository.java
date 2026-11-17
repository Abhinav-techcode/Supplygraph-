package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ImpactReassessmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataImpactReassessmentRepository extends JpaRepository<ImpactReassessmentJpaEntity, String> {
    List<ImpactReassessmentJpaEntity> findByWorkspaceIdOrderByReassessedAtDesc(String workspaceId);
}

