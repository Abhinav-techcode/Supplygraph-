package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.CarbonEmissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCarbonEmissionRepository extends JpaRepository<CarbonEmissionJpaEntity, String> {
    List<CarbonEmissionJpaEntity> findByWorkspaceId(String workspaceId);
}

