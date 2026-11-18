package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Multi-echelon inventory plan aggregate aligning safety stocks and reorder policies across the network.
 */
public record InventoryPlan(
        InventoryPlanId id,
        WorkspaceId workspaceId,
        String planName,
        ServiceLevel targetServiceLevel,
        List<EchelonInventoryPosition> positions,
        double totalCarryingCost,
        Instant calculatedAt
) implements Serializable {
    public InventoryPlan {
        Objects.requireNonNull(id, "Inventory plan id cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace id cannot be null");
        Objects.requireNonNull(planName, "Plan name cannot be null");
        Objects.requireNonNull(targetServiceLevel, "Target service level cannot be null");
        positions = List.copyOf(positions);
        Objects.requireNonNull(calculatedAt, "Calculation timestamp cannot be null");
    }

    public int getPositionCount() {
        return positions.size();
    }

    public long getAtRiskPositionCount() {
        return positions.stream().filter(EchelonInventoryPosition::isReorderRequired).count();
    }
}

