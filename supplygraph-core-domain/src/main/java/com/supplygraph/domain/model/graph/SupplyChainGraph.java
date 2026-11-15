package com.supplygraph.domain.model.graph;

import java.util.*;

/**
 * Immutable/Defensive representation of a multi-tier supply chain network graph.
 */
public class SupplyChainGraph {
    private final Map<NodeId, SupplyChainNode> nodes;
    private final Map<EdgeId, TransportationLane> lanes;
    private final Map<NodeId, Set<TransportationLane>> outgoingLanes;
    private final Map<NodeId, Set<TransportationLane>> incomingLanes;

    public SupplyChainGraph(Collection<SupplyChainNode> nodeList, Collection<TransportationLane> laneList) {
        Objects.requireNonNull(nodeList, "Nodes must not be null");
        Objects.requireNonNull(laneList, "Lanes must not be null");

        Map<NodeId, SupplyChainNode> nodeMap = new LinkedHashMap<>();
        for (SupplyChainNode node : nodeList) {
            if (nodeMap.containsKey(node.getId())) {
                throw new IllegalArgumentException("Duplicate node ID in graph: " + node.getId());
            }
            nodeMap.put(node.getId(), node);
        }
        this.nodes = Collections.unmodifiableMap(nodeMap);

        Map<EdgeId, TransportationLane> laneMap = new LinkedHashMap<>();
        Map<NodeId, Set<TransportationLane>> outMap = new HashMap<>();
        Map<NodeId, Set<TransportationLane>> inMap = new HashMap<>();

        for (NodeId nid : this.nodes.keySet()) {
            outMap.put(nid, new LinkedHashSet<>());
            inMap.put(nid, new LinkedHashSet<>());
        }

        for (TransportationLane lane : laneList) {
            if (laneMap.containsKey(lane.getId())) {
                throw new IllegalArgumentException("Duplicate edge ID in graph: " + lane.getId());
            }
            if (!this.nodes.containsKey(lane.getSourceNodeId())) {
                throw new IllegalArgumentException("Lane source node does not exist in graph: " + lane.getSourceNodeId());
            }
            if (!this.nodes.containsKey(lane.getTargetNodeId())) {
                throw new IllegalArgumentException("Lane target node does not exist in graph: " + lane.getTargetNodeId());
            }
            laneMap.put(lane.getId(), lane);
            outMap.get(lane.getSourceNodeId()).add(lane);
            inMap.get(lane.getTargetNodeId()).add(lane);
        }

        this.lanes = Collections.unmodifiableMap(laneMap);
        this.outgoingLanes = Collections.unmodifiableMap(outMap);
        this.incomingLanes = Collections.unmodifiableMap(inMap);
    }

    public static SupplyChainGraph empty() {
        return new SupplyChainGraph(List.of(), List.of());
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getLaneCount() {
        return lanes.size();
    }

    public boolean containsNode(NodeId nodeId) {
        return nodes.containsKey(nodeId);
    }

    public Optional<SupplyChainNode> findNode(NodeId nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public SupplyChainNode getNode(NodeId nodeId) {
        SupplyChainNode node = nodes.get(nodeId);
        if (node == null) {
            throw new NoSuchElementException("Node not found in graph: " + nodeId);
        }
        return node;
    }

    public Optional<TransportationLane> findLane(EdgeId edgeId) {
        return Optional.ofNullable(lanes.get(edgeId));
    }

    public Collection<SupplyChainNode> getAllNodes() {
        return nodes.values();
    }

    public Collection<TransportationLane> getAllLanes() {
        return lanes.values();
    }

    public Set<TransportationLane> getOutgoingLanes(NodeId nodeId) {
        Set<TransportationLane> set = outgoingLanes.get(nodeId);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    public Set<TransportationLane> getIncomingLanes(NodeId nodeId) {
        Set<TransportationLane> set = incomingLanes.get(nodeId);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    public Set<NodeId> getDirectPredecessors(NodeId nodeId) {
        Set<TransportationLane> in = getIncomingLanes(nodeId);
        Set<NodeId> predecessors = new LinkedHashSet<>();
        for (TransportationLane lane : in) {
            predecessors.add(lane.getSourceNodeId());
        }
        return Collections.unmodifiableSet(predecessors);
    }

    public Set<NodeId> getDirectSuccessors(NodeId nodeId) {
        Set<TransportationLane> out = getOutgoingLanes(nodeId);
        Set<NodeId> successors = new LinkedHashSet<>();
        for (TransportationLane lane : out) {
            successors.add(lane.getTargetNodeId());
        }
        return Collections.unmodifiableSet(successors);
    }

    /**
     * Traverses all upstream nodes reaching target node via DFS.
     */
    public Set<NodeId> getUpstreamReachableNodes(NodeId targetNodeId) {
        Set<NodeId> visited = new LinkedHashSet<>();
        Deque<NodeId> stack = new ArrayDeque<>();
        stack.push(targetNodeId);

        while (!stack.isEmpty()) {
            NodeId current = stack.pop();
            for (NodeId pred : getDirectPredecessors(current)) {
                if (visited.add(pred)) {
                    stack.push(pred);
                }
            }
        }
        return Collections.unmodifiableSet(visited);
    }

    /**
     * Traverses all downstream nodes reachable from source node via DFS.
     */
    public Set<NodeId> getDownstreamReachableNodes(NodeId sourceNodeId) {
        Set<NodeId> visited = new LinkedHashSet<>();
        Deque<NodeId> stack = new ArrayDeque<>();
        stack.push(sourceNodeId);

        while (!stack.isEmpty()) {
            NodeId current = stack.pop();
            for (NodeId succ : getDirectSuccessors(current)) {
                if (visited.add(succ)) {
                    stack.push(succ);
                }
            }
        }
        return Collections.unmodifiableSet(visited);
    }

    /**
     * Finds all simple paths between start and destination nodes.
     */
    public List<List<TransportationLane>> findAllPaths(NodeId start, NodeId destination) {
        List<List<TransportationLane>> result = new ArrayList<>();
        if (!containsNode(start) || !containsNode(destination)) {
            return result;
        }
        Set<NodeId> visited = new HashSet<>();
        visited.add(start);
        findPathsDfs(start, destination, visited, new ArrayList<>(), result);
        return result;
    }

    private void findPathsDfs(NodeId current, NodeId destination, Set<NodeId> visited,
                              List<TransportationLane> currentPath, List<List<TransportationLane>> result) {
        if (current.equals(destination)) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        for (TransportationLane lane : getOutgoingLanes(current)) {
            NodeId next = lane.getTargetNodeId();
            if (!visited.contains(next) && lane.isOperational()) {
                visited.add(next);
                currentPath.add(lane);
                findPathsDfs(next, destination, visited, currentPath, result);
                currentPath.remove(currentPath.size() - 1);
                visited.remove(next);
            }
        }
    }
}

