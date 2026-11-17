package com.supplygraph.domain.model.impact;

import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.NodeType;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Assessed impact on a specific graph node.
 */
public record NodeImpact(
        NodeId nodeId,
        String nodeName,
        NodeType nodeType,
        ImpactType impactType,
        String impactSummary,
        BigDecimal capacityLossPercentage
) {
    public NodeImpact {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        Objects.requireNonNull(nodeType, "NodeType must not be null");
        Objects.requireNonNull(impactType, "ImpactType must not be null");
        nodeName = nodeName == null ? nodeId.value() : nodeName;
        impactSummary = impactSummary == null ? "" : impactSummary;
        capacityLossPercentage = capacityLossPercentage == null ? BigDecimal.ZERO : capacityLossPercentage;
    }
}

