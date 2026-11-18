package com.supplygraph.adapter.persistence.repository.collaboration;

import com.supplygraph.adapter.persistence.entity.collaboration.PoCollaborationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPoCollaborationRepository extends JpaRepository<PoCollaborationJpaEntity, String> {
    List<PoCollaborationJpaEntity> findByWorkspaceId(String workspaceId);
}

