package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ConnectorExecutionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataConnectorExecutionRepository extends JpaRepository<ConnectorExecutionJpaEntity, String> {
    List<ConnectorExecutionJpaEntity> findByConnectorIdOrderByStartedAtDesc(String connectorId, Pageable page);
    List<ConnectorExecutionJpaEntity> findByWorkspaceIdOrderByStartedAtDesc(String workspaceId, Pageable page);
    boolean existsByRetryOf(String retryOf);
}
