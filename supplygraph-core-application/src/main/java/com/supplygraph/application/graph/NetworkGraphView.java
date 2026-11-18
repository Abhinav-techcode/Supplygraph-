package com.supplygraph.application.graph;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeType;

import java.time.Instant;
import java.util.List;

/** Read model of the digital twin for visualization and analysis. Everything here is derived from the persisted twin and active disruptions. */
public record NetworkGraphView(WorkspaceId workspaceId, long twinVersion, List<Node> nodes, List<Lane> lanes, List<Layer> layers,
                               List<DisruptionRef> activeDisruptions, Instant generatedAt) {
    public record Node(String id, String name, String location, NodeType type, String tierLabel, int layer, int row, boolean active,
                       boolean disrupted, List<String> disruptionTitles, double onHandUnits, String capacityText) {}
    public record Lane(String id, String fromId, String toId, String mode, long transitHours, boolean operational, boolean disrupted) {}
    public record Layer(int index, String label) {}
    public record DisruptionRef(String id, String title, String severity, int affectedNodes) {}

    public boolean isEmpty() { return nodes.isEmpty(); }
    public long disruptedNodeCount() { return nodes.stream().filter(Node::disrupted).count(); }
}
