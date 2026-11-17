package com.supplygraph.domain.model.impact;

import com.supplygraph.domain.model.bom.MaterialId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Quantified shortage impact for a specific raw material or component.
 */
public record MaterialShortageImpact(
        MaterialId materialId,
        String materialName,
        BigDecimal requiredQuantity,
        BigDecimal availableQuantity,
        BigDecimal shortageQuantity,
        String unitOfMeasure,
        Instant stockoutEstimatedTime
) {
    public MaterialShortageImpact {
        Objects.requireNonNull(materialId, "MaterialId must not be null");
        Objects.requireNonNull(requiredQuantity, "RequiredQuantity must not be null");
        Objects.requireNonNull(availableQuantity, "AvailableQuantity must not be null");
        Objects.requireNonNull(shortageQuantity, "ShortageQuantity must not be null");
        materialName = materialName == null ? materialId.value() : materialName;
        unitOfMeasure = unitOfMeasure == null ? "UNIT" : unitOfMeasure;
    }
}

