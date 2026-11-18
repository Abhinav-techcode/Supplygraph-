package com.supplygraph.adapter.persistence.repository.platform;

import com.supplygraph.adapter.persistence.entity.platform.ApiKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataApiKeyRepository extends JpaRepository<ApiKeyJpaEntity, String> {
    List<ApiKeyJpaEntity> findByWorkspaceId(String workspaceId);
    Optional<ApiKeyJpaEntity> findByHashedToken(String hashedToken);
}

