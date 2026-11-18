package com.supplygraph.domain.model.execution;

/** Executable task kinds. Each declares its adapter category, whether it is sensitive (extra approval) and its business compensation, if any. */
public enum TaskType {
    EXPEDITE_PURCHASE_ORDER(AdapterCategory.PROCUREMENT, true, "Cancel the expedite request and restore the original delivery date"),
    REQUEST_SUPPLIER_CAPACITY_CONFIRMATION(AdapterCategory.SUPPLIER_COMMUNICATION, false, "Withdraw the capacity request"),
    ALLOCATE_INVENTORY(AdapterCategory.INVENTORY, false, "Release the inventory allocation"),
    REASSIGN_PRODUCTION_CAPACITY(AdapterCategory.PRODUCTION, true, "Restore the previous capacity assignment"),
    CHANGE_PRODUCTION_SEQUENCE(AdapterCategory.PRODUCTION, false, "Restore the previous production sequence"),
    REQUEST_ALTERNATE_QUOTATION(AdapterCategory.SUPPLIER_COMMUNICATION, false, "Withdraw the quotation request"),
    REROUTE_SHIPMENT(AdapterCategory.LOGISTICS, false, "Cannot be reversed once the shipment has moved; requires a new reroute"),
    CHANGE_TRANSPORT_MODE(AdapterCategory.LOGISTICS, false, "Cannot be reversed once the shipment has moved; requires a new mode change"),
    TRIGGER_QUALITY_INSPECTION(AdapterCategory.QUALITY, false, "Cancel the pending inspection"),
    INITIATE_APPROVED_MATERIAL_SUBSTITUTION(AdapterCategory.PRODUCTION, true, null),
    NOTIFY_STAKEHOLDERS(AdapterCategory.NOTIFICATION, false, null);

    public enum AdapterCategory { PROCUREMENT, INVENTORY, PRODUCTION, LOGISTICS, SUPPLIER_COMMUNICATION, QUALITY, NOTIFICATION, ERP_COMMAND }

    private final AdapterCategory category;
    private final boolean sensitive;
    private final String compensation;

    TaskType(AdapterCategory category, boolean sensitive, String compensation) {
        this.category = category; this.sensitive = sensitive; this.compensation = compensation;
    }

    public AdapterCategory category() { return category; }
    public boolean sensitive() { return sensitive; }
    /** Description of the business compensation, or null when none exists. */
    public String compensation() { return compensation; }
    /** A real compensating action exists only when it does not say "cannot be reversed". */
    public boolean compensable() { return compensation != null && !compensation.startsWith("Cannot"); }
}
