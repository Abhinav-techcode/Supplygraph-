package com.supplygraph.domain.model.sustainability;

/**
 * Greenhouse Gas (GHG) Protocol defined emissions scopes.
 */
public enum EmissionScope {
    SCOPE_1_DIRECT("Scope 1: Direct Combustion & Fleet Operations"),
    SCOPE_2_INDIRECT_ELECTRICITY("Scope 2: Purchased Electricity, Steam & Cooling"),
    SCOPE_3_PURCHASED_GOODS("Scope 3 Category 1: Purchased Goods & Services"),
    SCOPE_3_UPSTREAM_FREIGHT("Scope 3 Category 4: Upstream Transportation & Logistics");

    private final String description;

    EmissionScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

