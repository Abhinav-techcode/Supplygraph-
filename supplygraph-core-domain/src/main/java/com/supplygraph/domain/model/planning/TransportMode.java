package com.supplygraph.domain.model.planning;

/**
 * Available freight transportation modes for network flow optimization.
 */
public enum TransportMode {
    ROAD("Over-the-Road Trucking (FTL/LTL)", 1.0, 62.0),
    RAIL("Intermodal Rail Freight", 0.45, 22.0),
    OCEAN("Ocean Container Vessel", 0.20, 11.0),
    AIR("Air Cargo Freight", 4.50, 602.0);

    private final String description;
    private final double relativeCostMultiplier;
    private final double gramsCo2PerTonKm;

    TransportMode(String description, double relativeCostMultiplier, double gramsCo2PerTonKm) {
        this.description = description;
        this.relativeCostMultiplier = relativeCostMultiplier;
        this.gramsCo2PerTonKm = gramsCo2PerTonKm;
    }

    public String getDescription() {
        return description;
    }

    public double getRelativeCostMultiplier() {
        return relativeCostMultiplier;
    }

    public double getGramsCo2PerTonKm() {
        return gramsCo2PerTonKm;
    }
}

