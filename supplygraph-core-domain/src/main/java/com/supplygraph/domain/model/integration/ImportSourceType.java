package com.supplygraph.domain.model.integration;

/**
 * Ingestion medium or external software interface origin for imported enterprise data.
 */
public enum ImportSourceType {
    CSV("Delimited Text File (CSV/TSV)", true),
    JSON("JavaScript Object Notation (JSON)", true),
    STRUCTURED_FORM("Manual Operator Entry Form", false),
    ERP_INTERFACE("Enterprise Resource Planning (ERP)", false),
    PLM_INTERFACE("Product Lifecycle Management (PLM)", false),
    WMS_INTERFACE("Warehouse Management System (WMS)", false),
    TMS_INTERFACE("Transportation Management System (TMS)", false);

    private final String description;
    private final boolean fileBased;

    ImportSourceType(String description, boolean fileBased) {
        this.description = description;
        this.fileBased = fileBased;
    }

    public String description() {
        return description;
    }

    public boolean isFileBased() {
        return fileBased;
    }
}
