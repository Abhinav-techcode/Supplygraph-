package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.SensitivityReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSensitivityReportRepository extends JpaRepository<SensitivityReportJpaEntity, String> {
    List<SensitivityReportJpaEntity> findByWorkspaceIdOrderByGeneratedAtDesc(String workspaceId);
}

