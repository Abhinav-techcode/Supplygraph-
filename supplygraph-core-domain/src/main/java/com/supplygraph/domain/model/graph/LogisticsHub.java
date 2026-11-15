package com.supplygraph.domain.model.graph;

import java.util.Objects;
import java.util.Set;

/**
 * Logistics and transshipment hub node.
 */
public class LogisticsHub extends SupplyChainNode {
    private final boolean crossDockingSupported;
    private final int dailyThroughputPallets;
    private final Set<String> supportedFreightModes;

    public LogisticsHub(NodeId id, String name, String location, boolean active,
                        boolean crossDockingSupported, int dailyThroughputPallets, Set<String> supportedFreightModes) {
        super(id, name, NodeType.LOGISTICS_HUB, location, active);
        if (dailyThroughputPallets < 0) {
            throw new IllegalArgumentException("Daily throughput pallets must be >= 0");
        }
        this.crossDockingSupported = crossDockingSupported;
        this.dailyThroughputPallets = dailyThroughputPallets;
        this.supportedFreightModes = supportedFreightModes == null ? Set.of() : Set.copyOf(supportedFreightModes);
    }

    public boolean isCrossDockingSupported() {
        return crossDockingSupported;
    }

    public int getDailyThroughputPallets() {
        return dailyThroughputPallets;
    }

    public Set<String> getSupportedFreightModes() {
        return supportedFreightModes;
    }
}

