package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.MaterialIntelligenceProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataMaterialProfileRepository extends JpaRepository<MaterialIntelligenceProfileJpaEntity, String> {
    List<MaterialIntelligenceProfileJpaEntity> findByWorkspaceId(String workspaceId);
    Optional<MaterialIntelligenceProfileJpaEntity> findByWorkspaceIdAndMaterialId(String workspaceId, String materialId);
}
