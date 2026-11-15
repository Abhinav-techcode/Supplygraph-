package com.supplygraph.domain.model.graph;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Storage and distribution warehouse node.
 */
public class Warehouse extends SupplyChainNode {
    private final BigDecimal storageCapacitySquareMeters;
    private final boolean coldStorageSupported;
    private final boolean hazardousMaterialAuthorized;

    public Warehouse(NodeId id, String name, String location, boolean active,
                     BigDecimal storageCapacitySquareMeters, boolean coldStorageSupported, boolean hazardousMaterialAuthorized) {
        super(id, name, NodeType.WAREHOUSE, location, active);
        Objects.requireNonNull(storageCapacitySquareMeters, "Storage capacity must not be null");
        if (storageCapacitySquareMeters.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Storage capacity must not be negative");
        }
        this.storageCapacitySquareMeters = storageCapacitySquareMeters;
        this.coldStorageSupported = coldStorageSupported;
        this.hazardousMaterialAuthorized = hazardousMaterialAuthorized;
    }

    public BigDecimal getStorageCapacitySquareMeters() {
        return storageCapacitySquareMeters;
    }

    public boolean isColdStorageSupported() {
        return coldStorageSupported;
    }

    public boolean isHazardousMaterialAuthorized() {
        return hazardousMaterialAuthorized;
    }
}

