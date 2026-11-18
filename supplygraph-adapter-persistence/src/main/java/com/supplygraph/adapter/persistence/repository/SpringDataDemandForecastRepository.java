package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.DemandForecastJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataDemandForecastRepository extends JpaRepository<DemandForecastJpaEntity, String> {
    List<DemandForecastJpaEntity> findByWorkspaceId(String workspaceId);
}

