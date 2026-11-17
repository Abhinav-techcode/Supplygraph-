package com.supplygraph.domain.model.disruption;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Estimated numerical and structural parameters of a disruption.
 */
public record DisruptionImpactParameters(
        BigDecimal capacityLossPercentage,
        Long expectedDelaySeconds,
        Integer leadTimeIncreaseDays,
        BigDecimal affectedQuantity,
        String unitOfMeasure,
        Map<String, String> customAttributes
) {
    public DisruptionImpactParameters {
        if (capacityLossPercentage != null) {
            if (capacityLossPercentage.compareTo(BigDecimal.ZERO) < 0 || capacityLossPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("capacityLossPercentage must be between 0 and 100: " + capacityLossPercentage);
            }
        }
        if (expectedDelaySeconds != null && expectedDelaySeconds < 0) {
            throw new IllegalArgumentException("expectedDelaySeconds must be non-negative: " + expectedDelaySeconds);
        }
        if (leadTimeIncreaseDays != null && leadTimeIncreaseDays < 0) {
            throw new IllegalArgumentException("leadTimeIncreaseDays must be non-negative: " + leadTimeIncreaseDays);
        }
        if (affectedQuantity != null && affectedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("affectedQuantity must be non-negative: " + affectedQuantity);
        }
        customAttributes = customAttributes == null ? Map.of() : Collections.unmodifiableMap(Map.copyOf(customAttributes));
    }

    public static DisruptionImpactParameters empty() {
        return new DisruptionImpactParameters(BigDecimal.ZERO, 0L, 0, BigDecimal.ZERO, "UNIT", Map.of());
    }

    public static DisruptionImpactParameters ofDelay(long delaySeconds) {
        return new DisruptionImpactParameters(BigDecimal.ZERO, delaySeconds, (int) (delaySeconds / 86400), BigDecimal.ZERO, "UNIT", Map.of());
    }

    public static DisruptionImpactParameters ofCapacityLoss(BigDecimal lossPercent) {
        return new DisruptionImpactParameters(lossPercent, 0L, 0, BigDecimal.ZERO, "PERCENT", Map.of());
    }
}

