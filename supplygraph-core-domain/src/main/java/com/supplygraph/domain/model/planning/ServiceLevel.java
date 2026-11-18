package com.supplygraph.domain.model.planning;

/**
 * Target cycle service levels and corresponding normal distribution z-scores for safety stock sizing.
 */
public enum ServiceLevel {
    SL_90("90.0% Service Level", 1.28155),
    SL_95("95.0% Service Level", 1.64485),
    SL_98("98.0% Service Level", 2.05375),
    SL_99("99.0% Service Level", 2.32635);

    private final String label;
    private final double zScore;

    ServiceLevel(String label, double zScore) {
        this.label = label;
        this.zScore = zScore;
    }

    public String getLabel() {
        return label;
    }

    public double getZScore() {
        return zScore;
    }
}

