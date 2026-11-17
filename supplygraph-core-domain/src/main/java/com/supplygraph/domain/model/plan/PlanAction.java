package com.supplygraph.domain.model.plan;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete recovery action proposed as part of a multi-step recovery plan.
 */
public record PlanAction(
        String actionId,
        PlanActionType actionType,
        String description,
        NodeId targetNodeId,
        MaterialId targetMaterialId,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal costEstimate,
        long estimatedDurationHours,
        Map<String, String> parameters
) {
    public PlanAction {
        Objects.requireNonNull(actionId, "ActionId must not be null");
        Objects.requireNonNull(actionType, "ActionType must not be null");
        description = description == null ? "" : description;
        quantity = quantity == null ? BigDecimal.ZERO : quantity;
        unitOfMeasure = unitOfMeasure == null ? "UNIT" : unitOfMeasure;
        costEstimate = costEstimate == null ? BigDecimal.ZERO : costEstimate;
        parameters = parameters == null ? Map.of() : Collections.unmodifiableMap(Map.copyOf(parameters));
    }
}

