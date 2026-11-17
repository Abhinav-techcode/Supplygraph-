package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, String> {
    List<AuditLogJpaEntity> findByWorkspaceIdOrderByTimestampDesc(String workspaceId);
    List<AuditLogJpaEntity> findByTargetEntityIdOrderByTimestampDesc(String targetEntityId);
}

