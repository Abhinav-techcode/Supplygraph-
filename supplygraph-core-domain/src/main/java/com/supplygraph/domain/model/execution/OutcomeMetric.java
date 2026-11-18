package com.supplygraph.domain.model.execution;

public enum OutcomeMetric {
    RECOVERY_DURATION_DAYS("days"), TOTAL_COST("cost"), SERVICE_LEVEL_PERCENT("%"), UNMET_DEMAND_QUANTITY("units");
    private final String unit;
    OutcomeMetric(String unit) { this.unit = unit; }
    public String unit() { return unit; }
}
