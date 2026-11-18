package com.supplygraph.adapter.persistence.repository.platform;

import com.supplygraph.adapter.persistence.entity.platform.WorkspaceQuotaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataWorkspaceQuotaRepository extends JpaRepository<WorkspaceQuotaJpaEntity, String> {
    Optional<WorkspaceQuotaJpaEntity> findByWorkspaceId(String workspaceId);
}

