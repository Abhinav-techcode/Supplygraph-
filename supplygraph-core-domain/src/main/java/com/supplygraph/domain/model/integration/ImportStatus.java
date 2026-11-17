package com.supplygraph.domain.model.integration;

/**
 * Execution lifecycle state of an enterprise data import batch job.
 */
public enum ImportStatus {
    PENDING_VALIDATION("Pending Schema & Domain Validation"),
    VALIDATED("Validated - Ready for Preview / Execution"),
    IN_PROGRESS("Import Execution in Progress"),
    COMPLETED("Successfully Imported"),
    PARTIALLY_COMPLETED("Partially Completed with Rejected Records"),
    REJECTED("Validation Failed - Rejected"),
    FAILED("Execution Error - Failed");

    private final String description;

    ImportStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == PARTIALLY_COMPLETED || this == REJECTED || this == FAILED;
    }
}
