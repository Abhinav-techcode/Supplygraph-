package com.supplygraph.domain.model.sensitivity;

/**
 * Key supply chain operational parameters subjected to sensitivity stress testing.
 */
public enum SensitivityParameter {
    LEAD_TIME("Transportation & Procurement Lead Time"),
    DISRUPTION_DURATION("Disruption Outage Duration"),
    DEMAND_SURGE("Customer Demand Volatility"),
    INVENTORY_AVAILABILITY("Buffer Inventory Availability"),
    YIELD_RATE("Manufacturing Yield & Defect Rate");

    private final String displayName;

    SensitivityParameter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

