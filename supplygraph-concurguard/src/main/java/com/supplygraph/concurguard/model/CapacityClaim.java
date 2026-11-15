package com.supplygraph.concurguard.model;

import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;

import java.util.Objects;

/**
 * Capacity claim on a processing resource.
 */
public record CapacityClaim(
        ResourceId resourceId,
        Quantity capacityUnits,
        CapacityWindow window
) {
    public CapacityClaim {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(capacityUnits, "capacityUnits must not be null");
        Objects.requireNonNull(window, "window must not be null");
    }

    public static CapacityClaim of(ResourceId resourceId, Quantity units, CapacityWindow window) {
        return new CapacityClaim(resourceId, units, window);
    }
}
