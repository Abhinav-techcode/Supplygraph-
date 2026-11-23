package com.supplygraph.application.agent;

import com.supplygraph.domain.model.bom.BomEntry;
import com.supplygraph.domain.model.bom.Material;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * Read-only facts about one incident, derived from a digital-twin snapshot. Every agent builds its proposal from these facts, so a
 * different twin or a different incident yields different proposals. Nothing here is a constant.
 */
public final class TwinInsights {
    public record StockHolder(NodeId node, double onHand) {}
    public record Route(List<NodeId> nodes, double hours, double costPerKg) {
        public String text() { return nodes.stream().map(NodeId::value).reduce((a, b) -> a + " > " + b).orElse(""); }
    }

    private final DigitalTwinSnapshot snapshot;
    private final DisruptionIncident incident;
    private final SupplyChainGraph graph;
    private final Set<NodeId> hit = new LinkedHashSet<>();
    private final Set<NodeId> downstream = new LinkedHashSet<>();

    private TwinInsights(DigitalTwinSnapshot snapshot, DisruptionIncident incident) {
        this.snapshot = snapshot;
        this.incident = incident;
        this.graph = snapshot.graph();
        for (String id : incident.affectedNodeIds()) {
            NodeId n = NodeId.of(id);
            if (graph.containsNode(n)) hit.add(n);
        }
        for (NodeId n : hit) downstream.addAll(graph.getDownstreamReachableNodes(n));
        downstream.removeAll(hit);
    }

    public static TwinInsights of(DigitalTwinSnapshot snapshot, DisruptionIncident incident) { return new TwinInsights(snapshot, incident); }

    /** Builds the incident view of a stored disruption (outage hours from its start/expected end; 72 h when no end is known). */
    public static DisruptionIncident incidentOf(com.supplygraph.domain.model.disruption.Disruption d) {
        long hours = d.expectedEndTime() != null ? Math.max(1, java.time.Duration.between(d.startTime(), d.expectedEndTime()).toHours()) : 72;
        return new DisruptionIncident(d.id().value(), d.workspaceId(), d.title(), d.description() == null ? "" : d.description(), d.industryCode(),
                d.affectedNodeIds().stream().map(NodeId::value).collect(java.util.stream.Collectors.toSet()),
                d.affectedMaterialIds().stream().map(MaterialId::value).collect(java.util.stream.Collectors.toSet()), hours, d.severity().name(), java.time.Instant.now());
    }

    public static TwinInsights of(AgentInvestigationContext ctx) { return new TwinInsights(ctx.snapshot(), ctx.incident()); }

    public SupplyChainGraph graph() { return graph; }
    public Set<NodeId> hitNodes() { return Collections.unmodifiableSet(hit); }
    public Set<NodeId> downstreamNodes() { return Collections.unmodifiableSet(downstream); }
    public boolean hasAffectedNodes() { return !hit.isEmpty(); }
    public DisruptionIncident incident() { return incident; }

    /** The first downstream manufacturing site (else first downstream node): whose supply is at risk. */
    public Optional<NodeId> exposedSite() {
        return downstream.stream().filter(n -> graph.getNode(n).getType() == NodeType.MANUFACTURING_SITE).sorted(Comparator.comparing(NodeId::value)).findFirst()
                .or(() -> downstream.stream().sorted(Comparator.comparing(NodeId::value)).findFirst());
    }

    public double throughputPerHour(NodeId n) {
        return graph.findNode(n).filter(x -> x instanceof ManufacturingSite).map(x -> (double) ((ManufacturingSite) x).getMaxThroughputPerHour()).orElse(0.0);
    }

    /** Declared material, else the material with the largest total stock in the network. */
    public Optional<MaterialId> material() {
        return incident.affectedMaterialIds().stream().map(MaterialId::of).findFirst().or(() -> {
            Map<MaterialId, Double> totals = new HashMap<>();
            snapshot.inventoryBalances().forEach((k, v) -> totals.merge(k.materialId(), v.amount().doubleValue(), Double::sum));
            return totals.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);
        });
    }

    public com.supplygraph.domain.model.common.UnitOfMeasure unit(MaterialId id) {
        return snapshot.bom().findMaterial(id).map(Material::getBaseUnit).orElse(com.supplygraph.domain.model.common.UnitOfMeasure.PIECE);
    }

    public String materialName(MaterialId id) {
        return snapshot.bom().findMaterial(id).map(Material::getName).orElse(id.value());
    }

    public double onHand(NodeId node, MaterialId material) {
        double sum = 0;
        for (Map.Entry<InventoryAccountId, Quantity> e : snapshot.inventoryBalances().entrySet())
            if (e.getKey().nodeId().equals(node) && e.getKey().materialId().equals(material)) sum += e.getValue().amount().doubleValue();
        return sum;
    }

    /** Estimated requirement over the outage: throughput/h x 24 x outage days of the exposed site. 0 when unknown. */
    public double requiredUnits() {
        double days = Math.max(1.0, incident.estimatedDowntimeHours() / 24.0);
        return exposedSite().map(this::throughputPerHour).orElse(0.0) * 24 * days;
    }

    /** Stock holders outside the disruption zone, largest first. */
    public List<StockHolder> stockHolders(MaterialId material) {
        List<StockHolder> list = new ArrayList<>();
        for (SupplyChainNode n : graph.getAllNodes()) {
            if (hit.contains(n.getId())) continue;
            double q = onHand(n.getId(), material);
            if (q > 0) list.add(new StockHolder(n.getId(), q));
        }
        list.sort(Comparator.comparingDouble(StockHolder::onHand).reversed());
        return list;
    }

    /** Days the exposed site can run from its own stock of the material. Empty when the requirement is unknown. */
    public OptionalDouble timeToSurviveDays(MaterialId material) {
        Optional<NodeId> site = exposedSite();
        if (site.isEmpty()) return OptionalDouble.empty();
        double perDay = throughputPerHour(site.get()) * 24;
        return perDay > 0 ? OptionalDouble.of(onHand(site.get(), material) / perDay) : OptionalDouble.empty();
    }

    /** Fastest route over operational lanes that never touches a disrupted node. */
    public Optional<Route> fastestRoute(NodeId from, NodeId to) {
        if (from.equals(to)) return Optional.empty();
        Map<NodeId, Double> dist = new HashMap<>();
        Map<NodeId, NodeId> prev = new HashMap<>();
        Map<NodeId, Double> cost = new HashMap<>();
        PriorityQueue<NodeId> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> dist.getOrDefault(n, Double.MAX_VALUE)));
        dist.put(from, 0.0);
        cost.put(from, 0.0);
        pq.add(from);
        Set<NodeId> done = new HashSet<>();
        while (!pq.isEmpty()) {
            NodeId cur = pq.poll();
            if (!done.add(cur)) continue;
            if (cur.equals(to)) break;
            for (TransportationLane l : graph.getOutgoingLanes(cur)) {
                NodeId next = l.getTargetNodeId();
                if (!l.isOperational() || hit.contains(next) || done.contains(next)) continue;
                double d = dist.get(cur) + l.getNominalTransitTime().toMinutes() / 60.0;
                if (d < dist.getOrDefault(next, Double.MAX_VALUE)) {
                    dist.put(next, d);
                    prev.put(next, cur);
                    cost.put(next, cost.get(cur) + l.getCostPerKg().doubleValue());
                    pq.add(next);
                }
            }
        }
        if (!dist.containsKey(to)) return Optional.empty();
        LinkedList<NodeId> path = new LinkedList<>();
        for (NodeId at = to; at != null; at = prev.get(at)) path.addFirst(at);
        return Optional.of(new Route(path, dist.get(to), cost.get(to)));
    }

    /** Suppliers outside the disruption zone that can reach the exposed site over undisrupted lanes, fastest first. */
    public List<Map.Entry<NodeId, Route>> alternativeSuppliers() {
        Optional<NodeId> site = exposedSite();
        if (site.isEmpty()) return List.of();
        List<Map.Entry<NodeId, Route>> out = new ArrayList<>();
        for (SupplyChainNode n : graph.getAllNodes()) {
            if (n.getType() != NodeType.SUPPLIER || hit.contains(n.getId())) continue;
            fastestRoute(n.getId(), site.get()).ifPresent(r -> out.add(Map.entry(n.getId(), r)));
        }
        out.sort(Comparator.comparingDouble(e -> e.getValue().hours()));
        return out;
    }

    /** Lanes leaving a disrupted node (the flows that stop). */
    public List<TransportationLane> blockedLanes() {
        List<TransportationLane> out = new ArrayList<>();
        for (NodeId n : hit) out.addAll(graph.getOutgoingLanes(n));
        out.sort(Comparator.comparing(l -> l.getId().value()));
        return out;
    }

    /** Facilities downstream that have no inbound lane from outside the disruption zone: single points of failure for the incident. */
    public List<NodeId> singlePointsOfFailure() {
        List<NodeId> out = new ArrayList<>();
        for (NodeId d : downstream) {
            Set<NodeId> preds = graph.getDirectPredecessors(d);
            if (!preds.isEmpty() && preds.stream().allMatch(p -> hit.contains(p) || downstream.contains(p))) out.add(d);
        }
        out.sort(Comparator.comparing(NodeId::value));
        return out;
    }

    /** Products that (transitively) consume the material according to the bill of materials, nearest first. */
    public List<MaterialId> dependentProducts(MaterialId material) {
        List<MaterialId> out = new ArrayList<>();
        Deque<MaterialId> queue = new ArrayDeque<>(List.of(material));
        Set<MaterialId> seen = new HashSet<>(List.of(material));
        while (!queue.isEmpty()) {
            MaterialId cur = queue.poll();
            for (BomEntry e : snapshot.bom().getDirectParents(cur)) {
                if (seen.add(e.parentMaterialId())) { out.add(e.parentMaterialId()); queue.add(e.parentMaterialId()); }
            }
        }
        return out;
    }

    public double networkDisruptedShare() {
        int total = graph.getNodeCount();
        return total == 0 ? 0 : (double) (hit.size() + downstream.size()) / total;
    }

    public com.supplygraph.domain.model.twin.StateVersion version() { return snapshot.version(); }
}
