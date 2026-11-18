package com.supplygraph.application.collaboration.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;

import java.util.List;
import java.util.Optional;

public interface PartnerOrganizationRepositoryPort {
    void save(PartnerOrganization organization);
    Optional<PartnerOrganization> findById(PartnerOrganizationId id);
    List<PartnerOrganization> findByWorkspaceId(WorkspaceId workspaceId);
}

