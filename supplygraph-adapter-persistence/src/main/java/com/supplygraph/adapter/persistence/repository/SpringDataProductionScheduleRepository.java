package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ProductionScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataProductionScheduleRepository extends JpaRepository<ProductionScheduleJpaEntity, String> {
    List<ProductionScheduleJpaEntity> findByWorkspaceId(String workspaceId);
}

