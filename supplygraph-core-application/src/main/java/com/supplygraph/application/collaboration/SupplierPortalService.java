package com.supplygraph.application.collaboration;

import com.supplygraph.application.collaboration.port.out.PartnerOrganizationRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.PartnerTier;
import com.supplygraph.domain.model.collaboration.PartnerType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierPortalService {

    private final PartnerOrganizationRepositoryPort partnerRepository;

    public SupplierPortalService(PartnerOrganizationRepositoryPort partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public PartnerOrganization onboardPartner(
            WorkspaceId workspaceId,
            String name,
            PartnerType type,
            PartnerTier tier,
            String dunsNumber,
            String countryCode
    ) {
        PartnerOrganization partner = new PartnerOrganization(
                PartnerOrganizationId.generate(),
                workspaceId,
                name,
                type,
                tier,
                dunsNumber,
                countryCode,
                true,
                Instant.now()
        );
        partnerRepository.save(partner);
        return partner;
    }

    public Optional<PartnerOrganization> getPartner(PartnerOrganizationId id) {
        return partnerRepository.findById(id);
    }

    public List<PartnerOrganization> listPartners(WorkspaceId workspaceId) {
        return partnerRepository.findByWorkspaceId(workspaceId);
    }
}

