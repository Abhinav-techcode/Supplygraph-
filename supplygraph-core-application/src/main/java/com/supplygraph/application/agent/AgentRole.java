package com.supplygraph.application.agent;

import com.supplygraph.concurguard.model.AgentId;

import java.util.Objects;

/**
 * Enumeration of the 8 specialized agents operating within the bounded SupplyGraph agentic layer.
 */
public enum AgentRole {
    COORDINATOR(AgentId.COORDINATOR, "Coordinator Agent", "Orchestrates multi-agent investigation, delegates tasks, and synthesizes overall recovery posture"),
    RISK_INTELLIGENCE(AgentId.RISK_INTELLIGENCE, "Risk Intelligence Agent", "Monitors external disruption signals, supplier health, and geopolitical risks"),
    INVENTORY(AgentId.INVENTORY, "Inventory Agent", "Evaluates buffer stocks, safety thresholds, and inventory allocation trade-offs"),
    PROCUREMENT(AgentId.PROCUREMENT, "Procurement Agent", "Identifies alternate suppliers, spot market buys, and purchase order expedites"),
    LOGISTICS(AgentId.LOGISTICS, "Logistics Agent", "Evaluates transportation lanes, multimodal routing, and carrier transit times"),
    PRODUCTION(AgentId.PRODUCTION, "Production Agent", "Re-plans manufacturing lines, batch changeovers, and capacity calendar allocations"),
    QUALITY(AgentId.QUALITY, "Quality Agent", "Enforces regulatory compliance, inspection hold policies, and cold-chain/cleanroom validation"),
    RECOVERY_PLANNING(AgentId.RECOVERY_PLANNING, "Recovery Planning Agent", "Synthesizes multi-criteria trade-offs and alternate end-to-end recovery strategies");

    private final AgentId agentId;
    private final String displayName;
    private final String description;

    AgentRole(AgentId agentId, String displayName, String description) {
        this.agentId = Objects.requireNonNull(agentId, "agentId must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
    }

    public AgentId agentId() {
        return agentId;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
