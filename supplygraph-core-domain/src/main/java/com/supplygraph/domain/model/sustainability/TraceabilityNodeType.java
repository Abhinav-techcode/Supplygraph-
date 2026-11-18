package com.supplygraph.domain.model.sustainability;

/**
 * Node classifications in the end-to-end supply chain genealogy DAG.
 */
public enum TraceabilityNodeType {
    RAW_MATERIAL_ORIGIN("Raw Material Origin / Mine / Farm"),
    SUPPLIER_BATCH("Supplier Manufactured Lot / Inbound Batch"),
    INVENTORY_RECEIPT("Receiving Dock Inspection & Warehouse Putaway"),
    WORK_ORDER_BATCH("Shop Floor Work Order Assembly Run"),
    FINISHED_PRODUCT_LOT("Finished Product Serialized Lot"),
    SHIPMENT_CONSIGNMENT("Customer Outbound Dispatch & Consignment");

    private final String description;

    TraceabilityNodeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

