package com.supplygraph.domain.model.integration;

/**
 * Categorization of supply chain enterprise datasets supported by the integration framework.
 */
public enum DataCategory {
    SUPPLIER("Supplier Master Data", "Supplier profiles, locations, lead times, capacity, and qualification records"),
    MATERIAL("Material Master Data", "Bill of materials components, specifications, criticality, and storage requirements"),
    INVENTORY("Inventory & Ledger Data", "Warehouse balances, lots, reserved quantities, expiry, and quarantine status"),
    PRODUCTION("Production & Capacity Data", "Manufacturing lines, planned production, daily capacity, and schedules"),
    LOGISTICS("Logistics & Transport Data", "Origin, destination, transportation lanes, lead times, and shipment commitments"),
    DEMAND("Customer & Demand Data", "Sales orders, delivery commitments, product requirements, and priority levels");

    private final String displayName;
    private final String description;

    DataCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}

