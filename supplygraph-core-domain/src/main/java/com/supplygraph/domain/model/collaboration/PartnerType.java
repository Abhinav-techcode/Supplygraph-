package com.supplygraph.domain.model.collaboration;

public enum PartnerType {
    SUPPLIER("Component and Raw Material Supplier"),
    LOGISTICS_CARRIER("3PL Freight and Logistics Carrier"),
    DISTRIBUTOR("Channel Partner and Regional Distributor"),
    RECYCLER("Circular Economy Recycler and Remanufacturer");

    private final String description;

    PartnerType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

