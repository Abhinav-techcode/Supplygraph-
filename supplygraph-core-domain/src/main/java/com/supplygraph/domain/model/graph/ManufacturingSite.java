package com.supplygraph.domain.model.graph;

import java.util.Objects;
import java.util.Set;

/**
 * Manufacturing facility node in the supply chain graph.
 */
public class ManufacturingSite extends SupplyChainNode {
    private final Set<String> productionLines;
    private final String cleanRoomClass;
    private final int maxThroughputPerHour;

    public ManufacturingSite(NodeId id, String name, String location, boolean active,
                             Set<String> productionLines, String cleanRoomClass, int maxThroughputPerHour) {
        super(id, name, NodeType.MANUFACTURING_SITE, location, active);
        if (maxThroughputPerHour < 0) {
            throw new IllegalArgumentException("Throughput per hour cannot be negative");
        }
        this.productionLines = productionLines == null ? Set.of() : Set.copyOf(productionLines);
        this.cleanRoomClass = cleanRoomClass;
        this.maxThroughputPerHour = maxThroughputPerHour;
    }

    public Set<String> getProductionLines() {
        return productionLines;
    }

    public String getCleanRoomClass() {
        return cleanRoomClass;
    }

    public int getMaxThroughputPerHour() {
        return maxThroughputPerHour;
    }
}

