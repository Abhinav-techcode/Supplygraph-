package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.TraceabilityNodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataTraceabilityNodeRepository extends JpaRepository<TraceabilityNodeJpaEntity, String> {
    List<TraceabilityNodeJpaEntity> findByWorkspaceId(String workspaceId);
    Optional<TraceabilityNodeJpaEntity> findByWorkspaceIdAndBatchNumber(String workspaceId, String batchNumber);
}

