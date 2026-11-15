package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.bom.MaterialId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Trackable inventory lot with batch metadata and expiration handling.
 */
public record InventoryLot(
        LotNumber lotNumber,
        MaterialId materialId,
        Instant manufacturedAt,
        Optional<ExpirationDate> expirationDate,
        String vendorBatchNumber
) {
    public InventoryLot {
        Objects.requireNonNull(lotNumber, "LotNumber must not be null");
        Objects.requireNonNull(materialId, "MaterialId must not be null");
        Objects.requireNonNull(manufacturedAt, "ManufacturedAt must not be null");
        Objects.requireNonNull(expirationDate, "ExpirationDate optional must not be null");
    }

    public static InventoryLot of(LotNumber lot, MaterialId materialId, Instant manufacturedAt, ExpirationDate expirationDate, String vendorBatch) {
        return new InventoryLot(lot, materialId, manufacturedAt, Optional.ofNullable(expirationDate), vendorBatch);
    }

    public static InventoryLot nonExpiring(LotNumber lot, MaterialId materialId, Instant manufacturedAt) {
        return new InventoryLot(lot, materialId, manufacturedAt, Optional.empty(), null);
    }

    public boolean isExpired(Instant asOf) {
        return expirationDate.map(exp -> exp.isExpiredAsOf(asOf)).orElse(false);
    }
}

