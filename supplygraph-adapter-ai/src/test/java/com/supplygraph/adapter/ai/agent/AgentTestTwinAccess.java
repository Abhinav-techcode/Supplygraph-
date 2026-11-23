package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.DisruptionIncident;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.Set;

/** Public access to the package-private test twin for other test packages. */
public final class AgentTestTwinAccess {
    public static DigitalTwinSnapshot snapshot(double hub, double plant) { return AgentTestTwin.snapshot(hub, plant); }
    public static DisruptionIncident incident(Set<String> nodes, long hours) { return AgentTestTwin.incident(nodes, hours); }
    private AgentTestTwinAccess() {}
}
