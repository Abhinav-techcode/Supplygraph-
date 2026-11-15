package com.supplygraph.concurguard.model;

import java.util.Objects;

/**
 * Identifier representing the generating specialist agent.
 */
public record AgentId(String value) {
    public AgentId {
        Objects.requireNonNull(value, "AgentId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("AgentId value must not be blank");
        }
    }

    public static final AgentId COORDINATOR = new AgentId("COORDINATOR");
    public static final AgentId RISK_INTELLIGENCE = new AgentId("RISK_INTELLIGENCE");
    public static final AgentId INVENTORY = new AgentId("INVENTORY");
    public static final AgentId PROCUREMENT = new AgentId("PROCUREMENT");
    public static final AgentId LOGISTICS = new AgentId("LOGISTICS");
    public static final AgentId PRODUCTION = new AgentId("PRODUCTION");
    public static final AgentId QUALITY = new AgentId("QUALITY");
    public static final AgentId RECOVERY_PLANNING = new AgentId("RECOVERY_PLANNING");

    public static AgentId of(String value) {
        return new AgentId(value);
    }
}
