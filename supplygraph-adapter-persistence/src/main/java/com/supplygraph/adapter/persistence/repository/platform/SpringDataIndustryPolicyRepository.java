package com.supplygraph.adapter.persistence.repository.platform;

import com.supplygraph.adapter.persistence.entity.platform.IndustryPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataIndustryPolicyRepository extends JpaRepository<IndustryPolicyJpaEntity, String> {
    List<IndustryPolicyJpaEntity> findByWorkspaceId(String workspaceId);
    List<IndustryPolicyJpaEntity> findByWorkspaceIdAndIndustryCode(String workspaceId, String industryCode);
}

