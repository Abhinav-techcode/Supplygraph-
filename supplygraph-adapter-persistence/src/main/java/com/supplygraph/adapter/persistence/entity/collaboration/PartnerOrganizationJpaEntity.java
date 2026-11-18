package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_partner_organizations")
public class PartnerOrganizationJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "partner_type", length = 64, nullable = false)
    private String partnerType;

    @Column(name = "partner_tier", length = 64, nullable = false)
    private String partnerTier;

    @Column(name = "duns_number", length = 64)
    private String dunsNumber;

    @Column(name = "country_code", length = 16, nullable = false)
    private String countryCode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "onboarded_at", nullable = false)
    private Instant onboardedAt;

    public PartnerOrganizationJpaEntity() {}

    public PartnerOrganizationJpaEntity(String id, String workspaceId, String name, String partnerType, String partnerTier, String dunsNumber, String countryCode, boolean active, Instant onboardedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.partnerType = partnerType;
        this.partnerTier = partnerTier;
        this.dunsNumber = dunsNumber;
        this.countryCode = countryCode;
        this.active = active;
        this.onboardedAt = onboardedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }

    public String getPartnerTier() { return partnerTier; }
    public void setPartnerTier(String partnerTier) { this.partnerTier = partnerTier; }

    public String getDunsNumber() { return dunsNumber; }
    public void setDunsNumber(String dunsNumber) { this.dunsNumber = dunsNumber; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getOnboardedAt() { return onboardedAt; }
    public void setOnboardedAt(Instant onboardedAt) { this.onboardedAt = onboardedAt; }
}

