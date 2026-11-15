package com.supplygraph.application.tool.builtin;

import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionRequest;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;
import java.util.function.Supplier;

/**
 * Read-only tool allowing bounded agents to query transportation lane status, transit times, and modes.
 */
public class QueryLaneStatusTool implements AgentTool {

    public static final String NAME = "query_lane_status";

    private final Supplier<DigitalTwinSnapshot> snapshotSupplier;

    public QueryLaneStatusTool(Supplier<DigitalTwinSnapshot> snapshotSupplier) {
        this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier, "snapshotSupplier must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Query transportation lane transit times, transport modes, costs, and operational status.";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request) {
        DigitalTwinSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null) {
            return ToolExecutionResult.failure("Digital twin snapshot is not available");
        }

        SupplyChainGraph graph = snapshot.graph();
        String laneIdStr = request.parameters().get("laneId") != null ? request.parameters().get("laneId").toString() : null;
        String originIdStr = request.parameters().get("originNodeId") != null ? request.parameters().get("originNodeId").toString() : null;
        String destinationIdStr = request.parameters().get("destinationNodeId") != null ? request.parameters().get("destinationNodeId").toString() : null;

        List<TransportationLane> matchingLanes = new ArrayList<>();

        if (laneIdStr != null && !laneIdStr.isBlank()) {
            TransportationLane lane = graph.getAllLanes().stream()
                    .filter(l -> l.getId().value().equalsIgnoreCase(laneIdStr))
                    .findFirst()
                    .orElse(null);
            if (lane != null) {
                matchingLanes.add(lane);
            }
        } else if (originIdStr != null && !originIdStr.isBlank()) {
            NodeId origin = NodeId.of(originIdStr);
            Set<TransportationLane> outgoing = graph.getOutgoingLanes(origin);
            if (destinationIdStr != null && !destinationIdStr.isBlank()) {
                NodeId dest = NodeId.of(destinationIdStr);
                outgoing.stream()
                        .filter(l -> l.getTargetNodeId().equals(dest))
                        .forEach(matchingLanes::add);
            } else {
                matchingLanes.addAll(outgoing);
            }
        } else {
            matchingLanes.addAll(graph.getAllLanes());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (TransportationLane lane : matchingLanes) {
            Map<String, Object> lMap = new HashMap<>();
            lMap.put("laneId", lane.getId().value());
            lMap.put("origin", lane.getSourceNodeId().value());
            lMap.put("destination", lane.getTargetNodeId().value());
            lMap.put("transportMode", lane.getTransportMode().name());
            lMap.put("transitTimeHours", lane.getNominalTransitTime().toHours());
            lMap.put("costPerKg", lane.getCostPerKg().toPlainString());
            lMap.put("operational", lane.isOperational());
            results.add(lMap);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("matchingLanesCount", results.size());
        responseData.put("lanes", results);

        return ToolExecutionResult.success(responseData);
    }
}
