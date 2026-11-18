package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ConnectorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataConnectorRepository extends JpaRepository<ConnectorJpaEntity, String> {
    List<ConnectorJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
