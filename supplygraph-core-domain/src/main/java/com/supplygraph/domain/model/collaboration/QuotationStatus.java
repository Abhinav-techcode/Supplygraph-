package com.supplygraph.domain.model.collaboration;

public enum QuotationStatus {
    SUBMITTED("Quotation Submitted by Partner"),
    UNDER_REVIEW("Under Review by Procurement"),
    ACCEPTED("Accepted and Awarded"),
    REJECTED("Declined");

    private final String description;

    QuotationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

