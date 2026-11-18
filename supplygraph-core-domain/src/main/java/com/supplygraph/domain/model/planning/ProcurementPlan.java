package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise procurement plan establishing component sourcing splits, MOQs, and total cost projections.
 */
public record ProcurementPlan(
        ProcurementPlanId id,
        WorkspaceId workspaceId,
        String planName,
        List<NetRequirementItem> netRequirements,
        List<SupplierAllocationShare> supplierAllocations,
        BigDecimal totalProjectedCost,
        Instant generatedAt
) implements Serializable {
    public ProcurementPlan {
        Objects.requireNonNull(id, "Procurement plan ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(planName, "Plan name cannot be null");
        netRequirements = List.copyOf(netRequirements);
        supplierAllocations = List.copyOf(supplierAllocations);
        Objects.requireNonNull(totalProjectedCost, "Total projected cost cannot be null");
        Objects.requireNonNull(generatedAt, "Generation timestamp cannot be null");
    }
}

