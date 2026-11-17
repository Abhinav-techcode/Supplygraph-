package com.supplygraph.domain.model.intelligence;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Deterministic intelligence profile evaluating material criticality, ledger stock segregation, and runway.
 */
public record MaterialIntelligenceProfile(
        String materialId,
        String materialName,
        String category,
        MaterialCriticalityLevel criticality,
        String singleSourceSupplierId,
        int leadTimeDays,
        BigDecimal onHandQuantity,
        BigDecimal availableQuantity,
        BigDecimal reservedQuantity,
        BigDecimal quarantinedQuantity,
        BigDecimal inTransitQuantity,
        BigDecimal dailyConsumptionRate,
        BigDecimal inventoryRunwayDays,
        int expiryRiskBatchCount,
        int downstreamProductCount,
        String industryClassification
) implements Serializable {

    public MaterialIntelligenceProfile {
        Objects.requireNonNull(materialId, "Material ID cannot be null");
        materialName = materialName != null ? materialName : materialId;
        category = category != null ? category : "GENERAL";
        criticality = criticality != null ? criticality : MaterialCriticalityLevel.MEDIUM;
        onHandQuantity = onHandQuantity != null ? onHandQuantity : BigDecimal.ZERO;
        availableQuantity = availableQuantity != null ? availableQuantity : BigDecimal.ZERO;
        reservedQuantity = reservedQuantity != null ? reservedQuantity : BigDecimal.ZERO;
        quarantinedQuantity = quarantinedQuantity != null ? quarantinedQuantity : BigDecimal.ZERO;
        inTransitQuantity = inTransitQuantity != null ? inTransitQuantity : BigDecimal.ZERO;
        dailyConsumptionRate = dailyConsumptionRate != null ? dailyConsumptionRate : BigDecimal.ZERO;
        inventoryRunwayDays = inventoryRunwayDays != null ? inventoryRunwayDays : BigDecimal.ZERO;
        industryClassification = industryClassification != null ? industryClassification : "CROSS_INDUSTRY";
    }

    public static BigDecimal calculateRunway(BigDecimal availableQty, BigDecimal dailyConsumption) {
        if (dailyConsumption == null || dailyConsumption.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(999); // Indefinite runway
        }
        if (availableQty == null || availableQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return availableQty.divide(dailyConsumption, 1, RoundingMode.HALF_UP);
    }

    public boolean isSingleSourced() {
        return singleSourceSupplierId != null && !singleSourceSupplierId.isBlank();
    }

    public boolean isRunwayBelowLeadTime() {
        return inventoryRunwayDays.compareTo(BigDecimal.valueOf(leadTimeDays)) < 0;
    }
}
