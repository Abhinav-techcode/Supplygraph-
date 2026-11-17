package com.supplygraph.domain.model.impact;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Assessed capacity constraint or reduction on a manufacturing site or resource.
 */
public record CapacityImpact(
        String resourceOrFacilityId,
        String facilityName,
        BigDecimal reductionPercentage,
        String timeWindowDescription
) {
    public CapacityImpact {
        Objects.requireNonNull(resourceOrFacilityId, "ResourceOrFacilityId must not be null");
        facilityName = facilityName == null ? resourceOrFacilityId : facilityName;
        reductionPercentage = reductionPercentage == null ? BigDecimal.ZERO : reductionPercentage;
        timeWindowDescription = timeWindowDescription == null ? "ACTIVE_DISRUPTION_WINDOW" : timeWindowDescription;
    }
}

