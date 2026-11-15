package com.supplygraph.domain.model.graph;

import java.util.Objects;
import java.util.Set;

/**
 * Supplier node in the supply chain graph.
 */
public class Supplier extends SupplyChainNode {
    private final int tier;
    private final Set<String> certifications;
    private double riskScore;

    public Supplier(NodeId id, String name, String location, boolean active, int tier, Set<String> certifications, double riskScore) {
        super(id, name, NodeType.SUPPLIER, location, active);
        if (tier < 1) {
            throw new IllegalArgumentException("Supplier tier must be >= 1");
        }
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("Risk score must be between 0.0 and 1.0");
        }
        this.tier = tier;
        this.certifications = certifications == null ? Set.of() : Set.copyOf(certifications);
        this.riskScore = riskScore;
    }

    public int getTier() {
        return tier;
    }

    public Set<String> getCertifications() {
        return certifications;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("Risk score must be between 0.0 and 1.0");
        }
        this.riskScore = riskScore;
    }
}

