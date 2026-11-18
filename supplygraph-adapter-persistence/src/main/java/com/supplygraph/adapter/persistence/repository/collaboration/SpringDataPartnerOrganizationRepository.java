package com.supplygraph.adapter.persistence.repository.collaboration;

import com.supplygraph.adapter.persistence.entity.collaboration.PartnerOrganizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPartnerOrganizationRepository extends JpaRepository<PartnerOrganizationJpaEntity, String> {
    List<PartnerOrganizationJpaEntity> findByWorkspaceId(String workspaceId);
}

