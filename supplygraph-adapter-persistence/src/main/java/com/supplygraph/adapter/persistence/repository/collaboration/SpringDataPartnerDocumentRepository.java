package com.supplygraph.adapter.persistence.repository.collaboration;

import com.supplygraph.adapter.persistence.entity.collaboration.PartnerDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPartnerDocumentRepository extends JpaRepository<PartnerDocumentJpaEntity, String> {
    List<PartnerDocumentJpaEntity> findByWorkspaceId(String workspaceId);
    List<PartnerDocumentJpaEntity> findByWorkspaceIdAndPartnerId(String workspaceId, String partnerId);
}

