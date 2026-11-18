package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.DecisionProvenanceEdgeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataDecisionProvenanceEdgeRepository extends JpaRepository<DecisionProvenanceEdgeJpaEntity, String> {
    List<DecisionProvenanceEdgeJpaEntity> findByDisruptionIdOrderByCreatedAtAsc(String disruptionId);
    List<DecisionProvenanceEdgeJpaEntity> findByWorkspaceIdOrderByCreatedAtAsc(String workspaceId);
}

