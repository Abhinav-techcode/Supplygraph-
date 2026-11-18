package com.supplygraph.application.collaboration.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerDocumentId;
import com.supplygraph.domain.model.collaboration.PartnerDocumentRecord;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;

import java.util.List;
import java.util.Optional;

public interface PartnerDocumentRepositoryPort {
    void save(PartnerDocumentRecord document);
    Optional<PartnerDocumentRecord> findById(PartnerDocumentId id);
    List<PartnerDocumentRecord> findByWorkspaceId(WorkspaceId workspaceId);
    List<PartnerDocumentRecord> findByPartnerId(WorkspaceId workspaceId, PartnerOrganizationId partnerId);
}

