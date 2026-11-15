package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Workspace entity persistence.
 */
@Repository
public interface SpringDataWorkspaceRepository extends JpaRepository<WorkspaceJpaEntity, String> {
    Optional<WorkspaceJpaEntity> findByName(String name);
}

