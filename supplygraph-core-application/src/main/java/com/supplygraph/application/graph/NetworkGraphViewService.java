package com.supplygraph.application.graph;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionStatus;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Builds the {@link NetworkGraphView}: layered layout (a node's layer is its longest distance from a source node, so any acyclic network
 * lays out left-to-right), disruption overlay from ACTIVE disruptions, on-hand inventory from the twin's ledger.
 */
@Service
public class NetworkGraphViewService {
    private final DigitalTwinRepositoryPort twins;
    private final DisruptionRepositoryPort disruptions;

    public NetworkGraphViewService(DigitalTwinRepositoryPort twins, DisruptionRepositoryPort disruptions) {
        this.twins = twins;
        this.disruptions = disruptions;
    }

    public NetworkGraphView build(WorkspaceId ws) {
        Optional<DigitalTwinState> found = twins.findByWorkspaceId(ws);
        if (found.isEmpty()) return new NetworkGraphView(ws, 0, List.of(), List.of(), List.of(), List.of(), Instant.now());
        DigitalTwinState twin = found.get();
        SupplyChainGraph g = twin.getGraph();

        List<Disruption> active = disruptions.findByWorkspaceIdAndStatus(ws, DisruptionStatus.ACTIVE);
        Map<NodeId, List<String>> byNode = new HashMap<>();
        Set<EdgeId> disruptedLanes = new HashSet<>();
        for (Disruption d : active) {
            d.affectedNodeIds().forEach(n -> byNode.computeIfAbsent(n, k -> new ArrayList<>()).add(d.title()));
            disruptedLanes.addAll(d.affectedLaneIds());
        }

        Map<NodeId, Integer> layerOf = layers(g);
        Map<Integer, List<SupplyChainNode>> byLayer = new TreeMap<>();
        for (SupplyChainNode n : g.getAllNodes()) byLayer.computeIfAbsent(layerOf.get(n.getId()), k -> new ArrayList<>()).add(n);

        Map<NodeId, Double> onHand = new HashMap<>();
        for (Map.Entry<InventoryAccountId, Quantity> e : twin.getInventoryLedger().getAllBalances().entrySet()) {
            if (e.getKey().category() == BalanceCategory.ON_HAND_AVAILABLE) onHand.merge(e.getKey().nodeId(), e.getValue().amount().doubleValue(), Double::sum);
        }

        List<NetworkGraphView.Node> nodes = new ArrayList<>();
        List<NetworkGraphView.Layer> layerList = new ArrayList<>();
        int col = 0;
        for (Map.Entry<Integer, List<SupplyChainNode>> le : byLayer.entrySet()) {
            List<SupplyChainNode> members = le.getValue();
            members.sort(Comparator.comparing((SupplyChainNode n) -> n.getType().ordinal()).thenComparing(n -> n.getId().value()));
            layerList.add(new NetworkGraphView.Layer(col, label(col, members)));
            int row = 0;
            for (SupplyChainNode n : members) {
                List<String> titles = byNode.getOrDefault(n.getId(), List.of());
                nodes.add(new NetworkGraphView.Node(n.getId().value(), n.getName(), n.getLocation(), n.getType(), tier(n), col, row++, n.isActive(),
                        !titles.isEmpty() || !n.isActive(), titles, onHand.getOrDefault(n.getId(), 0.0), capacity(n)));
            }
            col++;
        }
        List<NetworkGraphView.Lane> lanes = g.getAllLanes().stream().map(l -> new NetworkGraphView.Lane(l.getId().value(), l.getSourceNodeId().value(),
                l.getTargetNodeId().value(), l.getTransportMode().name(), l.getNominalTransitTime().toHours(), l.isOperational(),
                !l.isOperational() || disruptedLanes.contains(l.getId()))).toList();
        List<NetworkGraphView.DisruptionRef> refs = active.stream().map(d -> new NetworkGraphView.DisruptionRef(d.id().value(), d.title(), d.severity().name(), d.affectedNodeIds().size())).toList();
        return new NetworkGraphView(ws, twin.getCurrentVersion().value(), nodes, lanes, layerList, refs, Instant.now());
    }

    /** Longest-path layering over the lane DAG; nodes on a cycle (not expected) fall back to the layer reached before the cycle. */
    static Map<NodeId, Integer> layers(SupplyChainGraph g) {
        Map<NodeId, Integer> layer = new HashMap<>();
        Map<NodeId, Integer> indegree = new HashMap<>();
        for (SupplyChainNode n : g.getAllNodes()) { indegree.put(n.getId(), g.getIncomingLanes(n.getId()).size()); layer.put(n.getId(), 0); }
        Deque<NodeId> queue = new ArrayDeque<>();
        indegree.forEach((id, d) -> { if (d == 0) queue.add(id); });
        Set<NodeId> done = new HashSet<>();
        while (!queue.isEmpty()) {
            NodeId cur = queue.poll();
            done.add(cur);
            for (TransportationLane l : g.getOutgoingLanes(cur)) {
                NodeId t = l.getTargetNodeId();
                layer.merge(t, layer.get(cur) + 1, Math::max);
                if (indegree.merge(t, -1, Integer::sum) == 0) queue.add(t);
            }
        }
        // Normalise: renumber used layers consecutively.
        List<Integer> used = new ArrayList<>(new TreeSet<>(layer.values()));
        Map<NodeId, Integer> out = new HashMap<>();
        layer.forEach((id, l) -> out.put(id, used.indexOf(l)));
        return out;
    }

    private static String label(int index, List<SupplyChainNode> members) {
        Map<NodeType, Long> counts = new EnumMap<>(NodeType.class);
        members.forEach(n -> counts.merge(n.getType(), 1L, Long::sum));
        NodeType dominant = Collections.max(counts.entrySet(), Map.Entry.comparingByValue()).getKey();
        String kind = switch (dominant) {
            case SUPPLIER -> "SUPPLIERS";
            case MANUFACTURING_SITE -> "MANUFACTURING SITES";
            case WAREHOUSE -> "WAREHOUSES";
            case LOGISTICS_HUB -> "LOGISTICS HUBS";
        };
        return "STAGE " + (index + 1) + " · " + kind;
    }

    private static String tier(SupplyChainNode n) {
        return switch (n) {
            case Supplier s -> "Tier " + s.getTier();
            case ManufacturingSite m -> "Plant";
            case Warehouse w -> "Warehouse";
            case LogisticsHub h -> "Hub";
            default -> n.getType().name();
        };
    }

    private static String capacity(SupplyChainNode n) {
        return switch (n) {
            case ManufacturingSite m -> m.getMaxThroughputPerHour() + " units/h";
            case LogisticsHub h -> h.getDailyThroughputPallets() + " pallets/day";
            case Warehouse w -> w.getStorageCapacitySquareMeters().stripTrailingZeros().toPlainString() + " m²";
            case Supplier s -> s.getCertifications().isEmpty() ? "no certifications on record" : String.join(", ", new TreeSet<>(s.getCertifications()));
            default -> "n/a";
        };
    }
}
