package com.supplygraph.domain.model.plan;

/**
 * Types of discrete recovery actions that can be executed within a recovery plan.
 */
public enum PlanActionType {
    ALTERNATIVE_SUPPLIER,
    INVENTORY_REALLOCATION,
    PRODUCTION_RESCHEDULING,
    EXPEDITED_FREIGHT,
    CAPACITY_REASSIGNMENT,
    PURCHASE_ORDER_EXPEDITE,
    EMERGENCY_BUFFER_RELEASE
}

