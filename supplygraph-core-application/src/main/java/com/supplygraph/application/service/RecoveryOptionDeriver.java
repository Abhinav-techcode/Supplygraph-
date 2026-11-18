package com.supplygraph.application.service;

import com.supplygraph.domain.model.bom.Component;
import com.supplygraph.domain.model.bom.Material;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.PlanActionType;
import com.supplygraph.domain.model.plan.PlanComparisonMetrics;
import com.supplygraph.domain.model.twin.DigitalTwinState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Derives candidate recovery options from the digital twin and one disruption. Nothing here is a template: the holder of buffer stock,
 * the alternative supplier, quantities, transit time and transport cost all come from the twin's inventory ledger, graph lanes and BOM.
 * Estimation rules (stated in each option's description): requirement = throughput/hour x 24 x outage days of the first downstream
 * manufacturing site; transport cost = sum of the lane costs along the fastest usable path (per shipment; item prices are not modelled).
 * Lanes and nodes hit by the disruption are never used.
 */
public final class RecoveryOptionDeriver {
    public record Option(String name, String description, PlanAction action, PlanComparisonMetrics metrics) {}
    record Path(double hours, double costPerKg, List<NodeId> nodes) {}

    private RecoveryOptionDeriver() {}

    public static List<Option> derive(Disruption d, DigitalTwinState twin) {
        SupplyChainGraph g = twin.getGraph();
        Set<NodeId> hit = new HashSet<>();
        for (NodeId n : d.affectedNodeIds()) if (g.containsNode(n)) hit.add(n);
        if (hit.isEmpty()) return List.of();

        MaterialId material = d.affectedMaterialIds().stream().findFirst().orElseGet(() -> dominantMaterial(twin));
        if (material == null) return List.of();

        // The facility whose supply is at risk: first downstream manufacturing site, otherwise first downstream node.
        Set<NodeId> downstream = new LinkedHashSet<>();
        hit.forEach(n -> downstream.addAll(g.getDownstreamReachableNodes(n)));
        downstream.removeAll(hit);
        NodeId need = downstream.stream().filter(n -> g.getNode(n).getType() == NodeType.MANUFACTURING_SITE).sorted(Comparator.comparing(NodeId::value)).findFirst()
                .or(() -> downstream.stream().sorted(Comparator.comparing(NodeId::value)).findFirst()).orElse(null);
        if (need == null) return List.of();

        double outageDays = d.impactParameters() != null && d.impactParameters().expectedDelaySeconds() != null ? d.impactParameters().expectedDelaySeconds() / 86400.0 : 3.0;
        double throughput = g.getNode(need) instanceof ManufacturingSite m ? m.getMaxThroughputPerHour() : 0;
        double required = throughput > 0 ? throughput * 24 * Math.max(1.0, outageDays) : (d.impactParameters() != null && d.impactParameters().affectedQuantity() != null ? d.impactParameters().affectedQuantity().doubleValue() : 0);
        if (required <= 0) return List.of();

        Set<EdgeId> blockedLanes = new HashSet<>(d.affectedLaneIds());
        Map<NodeId, Double> onHand = onHand(twin, material);
        List<Option> options = new ArrayList<>();

        // Option 1: buffer stock from the best-placed holder.
        NodeId bestHolder = null;
        Path bestPath = null;
        for (Map.Entry<NodeId, Double> e : onHand.entrySet()) {
            if (hit.contains(e.getKey()) || e.getValue() <= 0 || e.getKey().equals(need)) continue;
            Path p = fastest(g, e.getKey(), need, hit, blockedLanes);
            if (p != null && (bestPath == null || p.hours() < bestPath.hours())) { bestHolder = e.getKey(); bestPath = p; }
        }
        if (bestHolder != null) {
            double qty = Math.min(required, onHand.get(bestHolder));
            double coverage = qty / required;
            BigDecimal cost = money(bestPath.costPerKg());
            PlanAction a = new PlanAction("ACT-BUFFER-" + bestHolder.value(), PlanActionType.EMERGENCY_BUFFER_RELEASE,
                    "Release " + fmt(qty) + " units of " + material.value() + " from " + bestHolder.value() + " to " + need.value() + " via " + route(bestPath),
                    need, material, BigDecimal.valueOf(Math.round(qty)), "PIECE", cost, Math.max(1, Math.round(bestPath.hours())),
                    Map.of("sourceNode", bestHolder.value(), "targetNode", need.value(), "route", route(bestPath), "pathHours", fmt(bestPath.hours()),
                            "estimate", "requirement=throughput*24*outageDays; cost=sum of lane costs along the route"));
            options.add(new Option("Buffer release from " + bestHolder.value(),
                    "Uses stock already on hand at " + bestHolder.value() + " (" + fmt(onHand.get(bestHolder)) + " units) and the fastest undisrupted route to " + need.value()
                            + ". Covers " + pct(coverage) + " of the estimated " + fmt(required) + "-unit requirement. Estimate rules: requirement = throughput/h x 24 x outage days; transport cost = sum of the lane costs along the route (per shipment; item prices are not modelled).",
                    a, new PlanComparisonMetrics(cost, bestPath.hours() / 24.0, 100.0 * coverage, BigDecimal.valueOf(Math.max(0, required - qty)).setScale(0, RoundingMode.HALF_UP),
                            BigDecimal.valueOf(Math.round(qty)), Math.min(100.0, 100.0 * qty / onHand.get(bestHolder)), Math.min(0.95, 0.10 + 0.5 * (1 - coverage)), Math.max(0.05, coverage))));
        }

        // Option 2: alternative supplier with a usable path and no disruption.
        NodeId bestSupplier = null;
        Path supplierPath = null;
        for (SupplyChainNode n : g.getAllNodes()) {
            if (n.getType() != NodeType.SUPPLIER || !n.isActive() || hit.contains(n.getId())) continue;
            Path p = fastest(g, n.getId(), need, hit, blockedLanes);
            if (p != null && (supplierPath == null || p.hours() < supplierPath.hours())) { bestSupplier = n.getId(); supplierPath = p; }
        }
        if (bestSupplier != null) {
            Supplier sup = (Supplier) g.getNode(bestSupplier);
            Material m = twin.getBom().getAllMaterials().stream().filter(x -> x.getId().equals(material)).findFirst().orElse(null);   // material may be absent from the BOM
            int leadDays = m instanceof Component c ? c.getProcurementLeadTimeDays() : 0;
            double hours = supplierPath.hours() + leadDays * 24.0;
            BigDecimal cost = money(supplierPath.costPerKg());
            PlanAction a = new PlanAction("ACT-RESOURCE-" + bestSupplier.value(), PlanActionType.ALTERNATIVE_SUPPLIER,
                    "Source " + fmt(required) + " units of " + material.value() + " from " + sup.getName() + " (" + bestSupplier.value() + ") via " + route(supplierPath),
                    bestSupplier, material, BigDecimal.valueOf(Math.round(required)), "PIECE", cost, Math.max(1, Math.round(hours)),
                    Map.of("supplierId", bestSupplier.value(), "targetNode", need.value(), "route", route(supplierPath), "pathHours", fmt(supplierPath.hours()), "procurementLeadDays", "" + leadDays,
                            "assumption", "supplier capacity is not modelled; confirm capacity before execution"));
            options.add(new Option("Alternative supplier " + bestSupplier.value(),
                    "Re-sources the requirement from " + sup.getName() + " (tier " + sup.getTier() + ", risk score " + String.format("%.2f", sup.getRiskScore()) + "): transit " + fmt(supplierPath.hours())
                            + " h plus " + leadDays + " day procurement lead time. The twin does not model supplier capacity, so capacity must be confirmed (see execution step).",
                    a, new PlanComparisonMetrics(cost, hours / 24.0, 100.0, BigDecimal.ZERO, BigDecimal.ZERO, 0, Math.min(0.95, Math.max(0.05, sup.getRiskScore() + 0.1)), Math.max(0.05, 1.0 - sup.getRiskScore()))));
        }

        // Option 3: stretch what the downstream site already holds by resequencing production.
        double local = onHand.getOrDefault(need, 0.0);
        if (local > 0) {
            double coverage = Math.min(1.0, local / required);
            PlanAction a = new PlanAction("ACT-RESEQUENCE-" + need.value(), PlanActionType.PRODUCTION_RESCHEDULING,
                    "Resequence production at " + need.value() + " to stretch " + fmt(local) + " units of on-hand " + material.value() + " over the outage",
                    need, material, BigDecimal.valueOf(Math.round(local)), "PIECE", BigDecimal.ZERO, Math.max(1, Math.round(outageDays * 24)),
                    Map.of("site", need.value(), "note", "cost of overtime/changeover is not modelled"));
            options.add(new Option("Resequence production at " + need.value(),
                    need.value() + " holds " + fmt(local) + " units, which covers " + pct(coverage) + " of the estimated requirement during the outage. Cost is not modelled (shown as 0), so compare with care.",
                    a, new PlanComparisonMetrics(BigDecimal.ZERO, outageDays, 100.0 * coverage, BigDecimal.valueOf(Math.max(0, required - local)).setScale(0, RoundingMode.HALF_UP),
                            BigDecimal.valueOf(Math.round(local)), 100.0, Math.min(0.95, 0.15 + 0.6 * (1 - coverage)), Math.max(0.05, coverage))));
        }
        return options;
    }

    private static MaterialId dominantMaterial(DigitalTwinState twin) {
        Map<MaterialId, Double> totals = new HashMap<>();
        for (Map.Entry<InventoryAccountId, Quantity> e : twin.getInventoryLedger().getAllBalances().entrySet())
            if (e.getKey().category() == BalanceCategory.ON_HAND_AVAILABLE) totals.merge(e.getKey().materialId(), e.getValue().amount().doubleValue(), Double::sum);
        return totals.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static Map<NodeId, Double> onHand(DigitalTwinState twin, MaterialId material) {
        Map<NodeId, Double> out = new LinkedHashMap<>();
        for (Map.Entry<InventoryAccountId, Quantity> e : twin.getInventoryLedger().getAllBalances().entrySet())
            if (e.getKey().category() == BalanceCategory.ON_HAND_AVAILABLE && e.getKey().materialId().equals(material)) out.merge(e.getKey().nodeId(), e.getValue().amount().doubleValue(), Double::sum);
        return out;
    }

    /** Dijkstra on transit hours over operational lanes that avoid disrupted lanes and nodes. */
    static Path fastest(SupplyChainGraph g, NodeId from, NodeId to, Set<NodeId> blockedNodes, Set<EdgeId> blockedLanes) {
        Map<NodeId, Double> dist = new HashMap<>();
        Map<NodeId, Double> cost = new HashMap<>();
        Map<NodeId, NodeId> prev = new HashMap<>();
        PriorityQueue<Map.Entry<NodeId, Double>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());
        dist.put(from, 0.0);
        cost.put(from, 0.0);
        pq.add(Map.entry(from, 0.0));
        while (!pq.isEmpty()) {
            var cur = pq.poll();
            NodeId u = cur.getKey();
            if (cur.getValue() > dist.getOrDefault(u, Double.MAX_VALUE)) continue;
            if (u.equals(to)) break;
            for (TransportationLane l : g.getOutgoingLanes(u)) {
                if (!l.isOperational() || blockedLanes.contains(l.getId()) || blockedNodes.contains(l.getTargetNodeId())) continue;
                double nd = dist.get(u) + l.getNominalTransitTime().toMinutes() / 60.0;
                if (nd < dist.getOrDefault(l.getTargetNodeId(), Double.MAX_VALUE)) {
                    dist.put(l.getTargetNodeId(), nd);
                    cost.put(l.getTargetNodeId(), cost.get(u) + l.getCostPerKg().doubleValue());
                    prev.put(l.getTargetNodeId(), u);
                    pq.add(Map.entry(l.getTargetNodeId(), nd));
                }
            }
        }
        if (!dist.containsKey(to)) return null;
        LinkedList<NodeId> path = new LinkedList<>();
        for (NodeId n = to; n != null; n = prev.get(n)) path.addFirst(n);
        return new Path(dist.get(to), cost.get(to), path);
    }

    private static String route(Path p) { return String.join(" > ", p.nodes().stream().map(NodeId::value).toList()); }
    private static BigDecimal money(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }
    private static String fmt(double v) { return String.format("%,.0f", v); }
    private static String pct(double v) { return String.format("%.0f%%", v * 100); }
}
