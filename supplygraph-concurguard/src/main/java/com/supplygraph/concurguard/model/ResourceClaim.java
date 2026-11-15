package com.supplygraph.concurguard.model;

import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;

import java.util.Objects;

/**
 * Explicit claim asserted by an ActionIntent on a canonical physical resource.
 */
public record ResourceClaim(
        ResourceIdentity resourceIdentity,
        ClaimType claimType,
        Quantity claimedQuantity,
        CapacityWindow claimWindow
) {
    public ResourceClaim {
        Objects.requireNonNull(resourceIdentity, "resourceIdentity must not be null");
        Objects.requireNonNull(claimType, "claimType must not be null");
        Objects.requireNonNull(claimedQuantity, "claimedQuantity must not be null");
        Objects.requireNonNull(claimWindow, "claimWindow must not be null");
    }

    public static ResourceClaim consumptive(ResourceIdentity identity, Quantity quantity, CapacityWindow window) {
        return new ResourceClaim(identity, ClaimType.CONSUMPTIVE, quantity, window);
    }

    public static ResourceClaim exclusive(ResourceIdentity identity, Quantity quantity, CapacityWindow window) {
        return new ResourceClaim(identity, ClaimType.EXCLUSIVE, quantity, window);
    }

    public static ResourceClaim shared(ResourceIdentity identity, Quantity quantity, CapacityWindow window) {
        return new ResourceClaim(identity, ClaimType.SHARED, quantity, window);
    }
}
