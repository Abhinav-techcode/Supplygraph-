package com.supplygraph.concurguard.model;

import java.util.Objects;

/**
 * Normalized, canonical identity of a physical supply chain asset.
 * Guarantees alias-free comparison across diverse agent representations.
 */
public record ResourceIdentity(
        String resourceType,   // "MACHINE", "LINE", "INVENTORY_ACCOUNT", "TRANSPORT_LANE", "WAREHOUSE_BAY"
        String canonicalId,    // e.g. "RES-LINE-01"
        String facilityId      // e.g. "MFG-GIGA-01"
) {
    public ResourceIdentity {
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        Objects.requireNonNull(canonicalId, "canonicalId must not be null");
        facilityId = facilityId == null ? "GLOBAL" : facilityId;
    }

    public static ResourceIdentity of(String resourceType, String canonicalId, String facilityId) {
        return new ResourceIdentity(resourceType, canonicalId, facilityId);
    }
}
