package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.DecisionProvenanceNodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataDecisionProvenanceNodeRepository extends JpaRepository<DecisionProvenanceNodeJpaEntity, String> {
    List<DecisionProvenanceNodeJpaEntity> findByDisruptionIdOrderByCreatedAtAsc(String disruptionId);
    List<DecisionProvenanceNodeJpaEntity> findByWorkspaceIdOrderByCreatedAtAsc(String workspaceId);
}
