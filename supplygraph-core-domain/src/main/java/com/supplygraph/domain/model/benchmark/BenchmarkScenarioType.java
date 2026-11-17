package com.supplygraph.domain.model.benchmark;

/**
 * Nine empirical evaluation scenarios for comparing coordination and replanning baselines.
 */
public enum BenchmarkScenarioType {
    SHARED_INVENTORY_CONTENTION(
            "Shared Inventory Contention",
            "Multiple bounded agents compete for the same safety stock buffer across product lines"
    ),
    SUPPLIER_CAPACITY_CONTENTION(
            "Supplier Capacity Contention",
            "Concurrent procurement recovery actions over-commit tier-1 supplier weekly delivery capacity"
    ),
    MACHINE_SCHEDULING_OVERLAP(
            "Machine Scheduling Overlap",
            "Alternative production runs schedule overlapping operating windows on a bottleneck work centre"
    ),
    CONFLICTING_LOGISTICS_ALLOCATION(
            "Conflicting Logistics Allocation",
            "Emergency freight bookings exceed aircraft/pallet payload constraints on shared transport lanes"
    ),
    SEMANTIC_RESOURCE_ALIASES(
            "Semantic Resource Aliases",
            "Agents refer to identical physical items by vendor code vs internal part number aliases"
    ),
    DEPENDENCY_CHAIN_CONFLICT(
            "Dependency Chain Conflict",
            "Upstream expediting creates inventory arrival after planned downstream assembly execution"
    ),
    STALE_STATE_PROPOSALS(
            "Stale-State Proposals",
            "Agent proposals generated against digital twin version V1 submitted after transition to V2"
    ),
    INDUSTRY_POLICY_VIOLATION(
            "Industry Policy Violation",
            "Candidate actions breach regulated cold-chain limits, cleanroom qualification, or dual-source rules"
    ),
    SIMULTANEOUS_DISRUPTIONS(
            "Simultaneous Disruptions",
            "Multiple concurrent regional disruptions trigger overlapping recovery agents simultaneously"
    );

    private final String displayName;
    private final String description;

    BenchmarkScenarioType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

