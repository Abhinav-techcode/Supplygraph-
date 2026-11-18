package com.supplygraph.domain.model.planning;

/**
 * Tactical supply chain planning scenario types evaluated before executive sign-off.
 */
public enum PlanningScenarioType {
    BASE_PLAN("Baseline Operational Plan"),
    DEMAND_SURGE("Demand Surge (+25% Volume)"),
    SUPPLY_RESTRICTION("Single-Source Disruption & Capacity Pinch"),
    ACCELERATED_DELIVERY("Accelerated Expedited Freight & Fast Changeovers");

    private final String description;

    PlanningScenarioType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

