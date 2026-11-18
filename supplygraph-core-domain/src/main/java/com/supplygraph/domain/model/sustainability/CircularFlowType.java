package com.supplygraph.domain.model.sustainability;

/**
 * Closed-loop material recovery and reverse logistics flow classifications.
 */
public enum CircularFlowType {
    CUSTOMER_RETURN("Customer Warranty Return / Core Intake"),
    REFURBISHMENT("Component Cleaning, Testing & Refurbishment"),
    REMANUFACTURING("Sub-Assembly Remanufacturing to As-New Spec"),
    BATTERY_CELL_RECYCLING("Black Mass Pyrometallurgical / Hydrometallurgical Recycling"),
    VIRGIN_MATERIAL_AVOIDANCE("Scrap Reclaimed into Secondary Feedstock");

    private final String description;

    CircularFlowType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

