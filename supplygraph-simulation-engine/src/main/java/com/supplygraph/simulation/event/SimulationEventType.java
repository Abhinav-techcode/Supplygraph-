package com.supplygraph.simulation.event;

/**
 * Supported discrete events modeled by the SupplyGraph simulation engine.
 */
public enum SimulationEventType {
    DISRUPTION_BEGINS,
    INVENTORY_BECOMES_UNAVAILABLE,
    SUPPLIER_DELIVERY_ARRIVES,
    PRODUCTION_STARTS,
    PRODUCTION_COMPLETES,
    CAPACITY_BECOMES_UNAVAILABLE,
    SHIPMENT_DEPARTS,
    SHIPMENT_ARRIVES,
    MATERIAL_SHORTAGE_OCCURS,
    RECOVERY_ACTION_TAKES_EFFECT,
    DISRUPTION_RESOLVES
}

