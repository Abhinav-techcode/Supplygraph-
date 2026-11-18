package com.supplygraph.domain.model.quality;

/**
 * Standard enterprise dimensions of data quality evaluated across the digital twin.
 */
public enum QualityDimension {
    COMPLETENESS("Completeness", "Verification of required fields, identifiers, and baseline operational attributes"),
    VALIDITY("Validity", "Conformity to domain syntax, ranges, positive quantities, and allowed value constraints"),
    CONSISTENCY("Consistency", "Logical harmony across interrelated attributes, double-entry balances, and accounts"),
    UNIQUENESS("Uniqueness", "Absence of duplicate identifiers, double-counted nodes, or redundant ledger postings"),
    REFERENTIAL_INTEGRITY("Referential Integrity", "Valid references between BOM components, nodes, lanes, and warehouses"),
    TEMPORAL_VALIDITY("Temporal Validity", "Chronological correctness, future calendar windows, and non-expired lots"),
    UNIT_CONSISTENCY("Unit Consistency", "Standardized physical units of measure (PIECE, KG, HOUR, LITRE) across relations"),
    PROVENANCE_COMPLETENESS("Provenance Completeness", "Explicit auditability of data sources, confidence indicators, and origin classification");

    private final String displayName;
    private final String description;

    QualityDimension(String displayName, String description) {
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

