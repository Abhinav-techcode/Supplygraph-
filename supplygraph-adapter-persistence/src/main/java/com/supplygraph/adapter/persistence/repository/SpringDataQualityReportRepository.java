package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.DataQualityReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataQualityReportRepository extends JpaRepository<DataQualityReportJpaEntity, String> {
    Optional<DataQualityReportJpaEntity> findFirstByWorkspaceIdOrderByEvaluatedAtDesc(String workspaceId);
    List<DataQualityReportJpaEntity> findByWorkspaceIdOrderByEvaluatedAtDesc(String workspaceId);
}

