package com.supplygraph.domain.model.collaboration;

public enum RfqStatus {
    DRAFT("Draft RFQ Specification"),
    PUBLISHED("Published to Partner Network"),
    CLOSED("Submissions Closed"),
    AWARDED("Contract Awarded to Selected Supplier"),
    CANCELLED("Cancelled");

    private final String description;

    RfqStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

