package com.supplygraph.domain.model.collaboration;

public enum PoStatus {
    ISSUED("Purchase Order Issued by Buyer"),
    ACKNOWLEDGED("Confirmed and Accepted by Supplier"),
    IN_PRODUCTION("Work in Progress at Supplier Facility"),
    SHIPPED("Goods Dispatched (ASN Generated)"),
    DELIVERED("Received and Inspected at Destination"),
    EXCEPTION("Delivery Exception / Delay Reported");

    private final String description;

    PoStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

