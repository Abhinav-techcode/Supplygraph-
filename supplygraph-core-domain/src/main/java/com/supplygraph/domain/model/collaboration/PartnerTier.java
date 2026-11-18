package com.supplygraph.domain.model.collaboration;

public enum PartnerTier {
    TIER_1_DIRECT("Tier-1 Direct OEM Supplier"),
    TIER_2_SUBTIER("Tier-2 Sub-Assembly / Intermediate Supplier"),
    TIER_3_RAW("Tier-3 Raw Material / Extraction Supplier");

    private final String description;

    PartnerTier(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

