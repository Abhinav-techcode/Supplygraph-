package com.supplygraph.domain.model.resilience.advanced;

import java.io.Serializable;
import java.util.Objects;

/**
 * Node-level survival, recovery, and path redundancy metric.
 */
public record NodeResilienceMetric(
        String nodeId,
        String nodeName,
        double localTTSDays,
        double localTTRDays,
        int alternatePathsCount,
        double dependencyCriticality
) implements Serializable {

    public NodeResilienceMetric {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(nodeName, "nodeName must not be null");
    }

    public static NodeResilienceMetric of(
            String nodeId,
            String nodeName,
            double localTTSDays,
            double localTTRDays,
            int alternatePathsCount,
            double dependencyCriticality
    ) {
        return new NodeResilienceMetric(nodeId, nodeName, localTTSDays, localTTRDays, alternatePathsCount, dependencyCriticality);
    }
}
