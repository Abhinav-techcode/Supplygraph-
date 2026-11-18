package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record PartnerOrganization(
        PartnerOrganizationId id,
        WorkspaceId workspaceId,
        String name,
        PartnerType type,
        PartnerTier tier,
        String dunsNumber,
        String countryCode,
        boolean active,
        Instant onboardedAt
) implements Serializable {
    public PartnerOrganization {
        Objects.requireNonNull(id, "Partner organization ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(name, "Organization name cannot be null");
        Objects.requireNonNull(type, "Partner type cannot be null");
        Objects.requireNonNull(tier, "Partner tier cannot be null");
        Objects.requireNonNull(countryCode, "Country code cannot be null");
        Objects.requireNonNull(onboardedAt, "Onboarding timestamp cannot be null");
    }
}

