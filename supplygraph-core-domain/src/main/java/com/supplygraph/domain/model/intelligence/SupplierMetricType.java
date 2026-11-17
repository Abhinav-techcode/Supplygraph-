package com.supplygraph.domain.model.intelligence;

/**
 * Deterministic quantitative metrics for assessing supplier risk, operational reliability, and dependencies.
 */
public enum SupplierMetricType {
    DELIVERY_RELIABILITY("Delivery Reliability", "Percentage of purchase orders delivered On-Time and In-Full (OTIF)", "%", true),
    LEAD_TIME_VARIABILITY("Lead Time Variability", "Standard deviation of actual delivery lead time versus contracted SLA", "Days", false),
    CAPACITY_UTILIZATION("Capacity Utilization", "Ratio of current committed volume to supplier max rated capacity", "%", false),
    SUPPLIER_CONCENTRATION_HHI("Supplier Concentration (HHI)", "Herfindahl-Hirschman Index market concentration score for supplier category", "Index", false),
    SINGLE_SOURCE_EXPOSURE("Single Source Exposure", "Number of critical components where this supplier is the sole qualified source", "Count", false),
    DISRUPTION_EXPOSURE("Disruption Exposure", "Composite exposure to geopolitical, environmental, or logistical hazard zones", "Score", false),
    ALTERNATIVE_AVAILABILITY("Alternative Supplier Availability", "Count of approved secondary or tertiary backup suppliers in the digital twin", "Count", true),
    HISTORICAL_PERFORMANCE("Historical Delivery Score", "Weighted aggregate of historical quality, fulfillment, and responsiveness", "Score", true);

    private final String displayName;
    private final String description;
    private final String unit;
    private final boolean higherIsBetter;

    SupplierMetricType(String displayName, String description, String unit, boolean higherIsBetter) {
        this.displayName = displayName;
        this.description = description;
        this.unit = unit;
        this.higherIsBetter = higherIsBetter;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String unit() {
        return unit;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }
}
