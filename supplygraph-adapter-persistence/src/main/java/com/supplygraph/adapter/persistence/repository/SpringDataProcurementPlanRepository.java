package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ProcurementPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataProcurementPlanRepository extends JpaRepository<ProcurementPlanJpaEntity, String> {
    List<ProcurementPlanJpaEntity> findByWorkspaceId(String workspaceId);
}

