package com.supplygraph.concurguard.model;

/**
 * Domain-specific recovery action category proposed by agents.
 */
public enum ActionType {
    EXPEDITE_PURCHASE_ORDER,
    REROUTE_SHIPMENT,
    SPLIT_PRODUCTION_BATCH,
    REALLOCATE_INVENTORY,
    RESERVE_CAPACITY,
    QUARANTINE_LOT,
    SUBSTITUTE_MATERIAL,
    HOLD_SHIPMENT,
    AUTHORIZE_OVERTIME_SHIFT,
    EMERGENCY_BUFFER_DISPATCH
}
