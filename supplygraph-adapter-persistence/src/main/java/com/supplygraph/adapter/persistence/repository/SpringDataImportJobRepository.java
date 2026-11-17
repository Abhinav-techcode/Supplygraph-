package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ImportJobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataImportJobRepository extends JpaRepository<ImportJobJpaEntity, String> {
    List<ImportJobJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
