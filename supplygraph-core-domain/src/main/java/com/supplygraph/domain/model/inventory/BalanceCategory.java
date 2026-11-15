package com.supplygraph.domain.model.inventory;

public enum BalanceCategory {
    ON_HAND_AVAILABLE,
    RESERVED_PRODUCTION,
    RESERVED_SHIPMENT,
    IN_TRANSIT,
    QUARANTINED,
    EXTERNAL_OFFSET // Double-entry balancing offset for initial receipts and final external consumption
}

