package com.supplygraph.domain.model.event;

/**
 * Standardized operational event types for continuous monitoring.
 */
public enum EventType {
    SUPPLIER_CAPACITY_CHANGED("Supplier available capacity updated"),
    SUPPLIER_DELIVERY_DELAYED("Inbound delivery shipment delayed"),
    INVENTORY_LEVEL_CHANGED("Physical on-hand inventory adjusted"),
    SHIPMENT_STATUS_CHANGED("Transportation shipment milestone reached"),
    PRODUCTION_CAPACITY_CHANGED("Plant line throughput adjusted"),
    QUALITY_HOLD_RECORDED("Batch or lot placed on quality containment"),
    DEMAND_CHANGED("Customer demand schedule modified"),
    DISRUPTION_RESOLVED("Operational incident declared resolved"),
    RECOVERY_ACTION_COMPLETED("Recovery action executed and verified");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

