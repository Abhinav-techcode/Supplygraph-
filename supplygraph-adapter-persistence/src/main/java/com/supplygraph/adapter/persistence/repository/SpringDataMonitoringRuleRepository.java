package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.MonitoringRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataMonitoringRuleRepository extends JpaRepository<MonitoringRuleJpaEntity, String> {
    List<MonitoringRuleJpaEntity> findByIndustryApplicabilityOrIndustryApplicability(String industry, String all);
}

