package com.supplygraph.simulation.engine;

import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.ManufacturingSite;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.NodeType;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.graph.SupplyChainNode;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.PlanActionType;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.simulation.event.SimulationEvent;
import com.supplygraph.simulation.event.SimulationEventType;
import com.supplygraph.simulation.state.SimulatedDigitalTwinState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic day-by-day stock simulation of the digital twin under a disruption, with or without a recovery plan.
 * Runs on an isolated copy of the twin and never touches the real ledger.
 *
 * <p>The model, in plain terms:
 * <ul>
 *   <li>Every manufacturing site consumes (stated throughput per hour x 24) units a day of each material it holds.</li>
 *   <li>In normal running, supply arriving over the lanes equals that consumption, so stock stays level. The twin records no flow rates, so this is an assumption.</li>
 *   <li>A site is "supplied" while an active supplier can reach it over operational lanes that avoid disrupted nodes and lanes. During the outage a site that is cut off
 *       receives nothing and runs down its own stock.</li>
 *   <li>A disrupted site itself produces only (100% - capacity loss) of its demand.</li>
 *   <li>Recovery actions move real stock: a buffer release takes units from the source site and adds them to the target; an alternative supplier restores supply
 *       to the site once its lead time has passed, up to the quantity the plan commits to. Other action types are logged but not modelled.</li>
 *   <li>Service level = units consumed / units demanded over the run. Nothing is estimated beyond the assumptions above.</li>
 * </ul>
 */
public class DiscreteEventSimulationEngine {
    private static final long DAY = 86400L;

    public SimulationResult run(DigitalTwinSnapshot snapshot, Disruption disruption, RecoveryPlan recoveryPlan, SimulationConfiguration config) {
        Objects.requireNonNull(snapshot, "DigitalTwinSnapshot must not be null");
        Objects.requireNonNull(disruption, "Disruption must not be null");
        Objects.requireNonNull(config, "SimulationConfiguration must not be null");

        String simId = "SIM-" + UUID.randomUUID().toString().substring(0, 8);
        SupplyChainGraph g = snapshot.graph();
        SimulatedDigitalTwinState state = SimulatedDigitalTwinState.fromSnapshot(snapshot);
        List<SimulationEvent> events = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> assumptions = new ArrayList<>(List.of(
                "Each manufacturing site consumes throughput per hour x 24 units a day of every material it holds.",
                "In normal running, inbound supply equals consumption (the twin records no flow rates), so stock stays level.",
                "A site is supplied while an active supplier can reach it over operational lanes that avoid disrupted nodes and lanes; a cut-off site receives nothing.",
                "The outage is simulated as starting at the beginning of the run and lasting the incident's expected delay.",
                "Stock held at hubs and warehouses only moves when a recovery plan moves it."));
        int[] eventNo = {0};

        // Outage window.
        Instant runStart = config.startTime();
        Instant outageStart = disruption.startTime().isBefore(runStart) ? runStart : disruption.startTime();
        Long delay = disruption.impactParameters() == null ? null : disruption.impactParameters().expectedDelaySeconds();
        Instant outageEnd = delay != null && delay > 0 ? outageStart.plusSeconds(delay)
                : disruption.expectedEndTime() != null && disruption.expectedEndTime().isAfter(outageStart) ? disruption.expectedEndTime() : config.endTime();
        double lossFraction = disruption.impactParameters() == null || disruption.impactParameters().capacityLossPercentage() == null ? 1.0
                : Math.max(0.0, Math.min(1.0, disruption.impactParameters().capacityLossPercentage().doubleValue() / 100.0));

        Set<NodeId> hitNodes = new HashSet<>(disruption.affectedNodeIds());
        Set<EdgeId> hitLanes = new HashSet<>(disruption.affectedLaneIds());
        Set<NodeId> suppliedNormally = suppliedSites(g, Set.of(), Set.of());
        Set<NodeId> suppliedInOutage = suppliedSites(g, hitNodes, hitLanes);

        // Sites and the materials they hold: these are where demand exists.
        Map<String, Double> stock = new TreeMap<>();
        Map<NodeId, Set<String>> siteMaterials = new TreeMap<>(Comparator.comparing(NodeId::value));
        snapshot.inventoryBalances().forEach((acct, qty) -> {
            if (acct.category() != BalanceCategory.ON_HAND_AVAILABLE) return;
            stock.merge(acct.nodeId().value() + ":" + acct.materialId().value(), qty.amount().doubleValue(), Double::sum);
            if (g.findNode(acct.nodeId()).orElse(null) instanceof ManufacturingSite site && site.isActive() && site.getMaxThroughputPerHour() > 0) {
                siteMaterials.computeIfAbsent(acct.nodeId(), k -> new TreeSet<>()).add(acct.materialId().value());
            }
        });
        stock.forEach((key, value) -> {
            int i = key.indexOf(':');
            state.adjustInventory(key.substring(0, i), key.substring(i + 1), BigDecimal.valueOf(value), runStart);   // opening balance, day 0
        });
        if (siteMaterials.isEmpty()) warnings.add("No active manufacturing site holds stock in the twin, so there is no demand to simulate and the service level is shown as 100%.");

        // Recovery actions, scheduled at the plan's own lead time after the outage begins.
        List<Object[]> scheduled = new ArrayList<>();   // {Instant, PlanAction}
        Map<String, Double> supplyPool = new HashMap<>();   // plant:material -> units still to be delivered by an alternative supplier
        if (config.includeRecoveryPlan() && recoveryPlan != null) {
            for (PlanAction action : recoveryPlan.actions()) {
                Instant at = outageStart.plusSeconds(Math.max(0, action.estimatedDurationHours()) * 3600L);
                if (at.isBefore(config.endTime())) scheduled.add(new Object[]{at, action});
                else warnings.add("\"" + action.description() + "\" would only take effect after the end of this run.");
            }
            scheduled.sort(Comparator.comparing(a -> (Instant) a[0]));
        }

        events.add(new SimulationEvent("EVT-" + (++eventNo[0]), outageStart, 0, SimulationEventType.DISRUPTION_BEGINS, disruption.id().value(), disruption.title(),
                Map.of("category", disruption.category().name(), "severity", disruption.severity().name())));
        if (outageEnd.isBefore(config.endTime())) {
            events.add(new SimulationEvent("EVT-" + (++eventNo[0]), outageEnd, 1, SimulationEventType.DISRUPTION_RESOLVES, disruption.id().value(), "Disruption expected to be resolved", Map.of()));
        }

        long totalDays = Math.max(1, (Duration.between(runStart, config.endTime()).getSeconds() + DAY - 1) / DAY);
        double demandTotal = 0, consumedTotal = 0, shortageTotal = 0;
        Map<String, Integer> shortRun = new HashMap<>();
        int longestRun = 0;
        int scheduleIdx = 0;

        for (int d = 0; d < totalDays; d++) {
            Instant dayStart = runStart.plusSeconds(d * DAY);
            Instant dayEnd = dayStart.plusSeconds(DAY);
            boolean outage = !dayStart.isBefore(outageStart) && dayStart.isBefore(outageEnd);

            while (scheduleIdx < scheduled.size() && ((Instant) scheduled.get(scheduleIdx)[0]).isBefore(dayEnd)) {
                Instant at = (Instant) scheduled.get(scheduleIdx)[0];
                PlanAction action = (PlanAction) scheduled.get(scheduleIdx)[1];
                scheduleIdx++;
                applyAction(action, at, stock, supplyPool, state, events, eventNo, assumptions);
            }

            for (Map.Entry<NodeId, Set<String>> site : siteMaterials.entrySet()) {
                NodeId plant = site.getKey();
                int perHour = ((ManufacturingSite) g.getNode(plant)).getMaxThroughputPerHour();
                double demand = perHour * 24.0;
                double running = outage && hitNodes.contains(plant) ? 1.0 - lossFraction : 1.0;
                double need = demand * running;
                boolean supplied = outage ? suppliedInOutage.contains(plant) : suppliedNormally.contains(plant);
                for (String material : site.getValue()) {
                    String key = plant.value() + ":" + material;
                    double before = stock.getOrDefault(key, 0.0);
                    double inflow = supplied ? need : 0.0;
                    double pool = supplyPool.getOrDefault(key, 0.0);
                    if (!supplied && pool > 0) {
                        double extra = Math.min(need, pool);
                        inflow += extra;
                        supplyPool.put(key, pool - extra);
                    }
                    double available = before + inflow;
                    double consumed = Math.min(available, need);
                    double after = available - consumed;
                    double unmet = demand - consumed;   // includes demand a damaged site cannot serve at all
                    stock.put(key, after);
                    demandTotal += demand;
                    consumedTotal += consumed;
                    shortageTotal += unmet;
                    if (Math.abs(after - before) > 1e-9) state.adjustInventory(plant.value(), material, BigDecimal.valueOf(after - before), dayEnd.minusSeconds(1));
                    if (unmet > 0.5) {
                        state.recordShortage(plant.value(), material, BigDecimal.valueOf(unmet), dayStart);
                        int run = shortRun.merge(key, 1, Integer::sum);
                        longestRun = Math.max(longestRun, run);
                        if (run == 1) {
                            events.add(new SimulationEvent("EVT-" + (++eventNo[0]), dayStart, 3, SimulationEventType.MATERIAL_SHORTAGE_OCCURS, plant.value(),
                                    plant.value() + " starts running short of " + material + " (" + Math.round(unmet) + " units unmet that day)",
                                    Map.of("material", material)));
                        }
                    } else {
                        shortRun.remove(key);
                    }
                }
            }
        }

        events.sort(Comparator.naturalOrder());
        double serviceLevel = demandTotal <= 0 ? 100.0 : 100.0 * consumedTotal / demandTotal;
        return new SimulationResult(simId, disruption.scenarioType(), snapshot.version(), runStart, config.endTime(), events.size(), events,
                state.getInventoryTrajectories(), state.getProductionTrajectories(), state.getShortageTrajectories(),
                serviceLevel, BigDecimal.valueOf(shortageTotal).setScale(0, RoundingMode.HALF_UP), longestRun * DAY, "COMPLETED", warnings, assumptions);
    }

    private static void applyAction(PlanAction action, Instant at, Map<String, Double> stock, Map<String, Double> supplyPool, SimulatedDigitalTwinState state,
                                    List<SimulationEvent> events, int[] eventNo, List<String> assumptions) {
        String material = action.targetMaterialId() == null ? null : action.targetMaterialId().value();
        double qty = action.quantity() == null ? 0 : action.quantity().doubleValue();
        Map<String, String> p = action.parameters() == null ? Map.of() : action.parameters();
        String target = p.getOrDefault("targetNode", action.targetNodeId() == null ? null : action.targetNodeId().value());
        boolean modelled = false;
        if (material != null && target != null && qty > 0) {
            switch (action.actionType()) {
                case EMERGENCY_BUFFER_RELEASE, INVENTORY_REALLOCATION -> {
                    String source = p.get("sourceNode");
                    String srcKey = source == null ? null : source + ":" + material;
                    double moved = srcKey == null ? 0 : Math.min(qty, stock.getOrDefault(srcKey, 0.0));   // never more than the source really holds
                    if (moved > 0) {
                        stock.merge(srcKey, -moved, Double::sum);
                        stock.merge(target + ":" + material, moved, Double::sum);
                        state.adjustInventory(source, material, BigDecimal.valueOf(-moved), at);
                        state.adjustInventory(target, material, BigDecimal.valueOf(moved), at);
                        modelled = true;
                    }
                }
                case ALTERNATIVE_SUPPLIER -> {
                    supplyPool.merge(target + ":" + material, qty, Double::sum);
                    modelled = true;
                }
                default -> { }
            }
        }
        if (!modelled && !assumptions.contains("Action type " + action.actionType() + " is logged but has no modelled effect on stock.")) {
            assumptions.add("Action type " + action.actionType() + " is logged but has no modelled effect on stock.");
        }
        events.add(new SimulationEvent("EVT-" + (++eventNo[0]), at, 2, SimulationEventType.RECOVERY_ACTION_TAKES_EFFECT, target == null ? "GLOBAL" : target, action.description(),
                Map.of("actionType", action.actionType().name(), "quantity", action.quantity() == null ? BigDecimal.ZERO : action.quantity())));
    }

    /** Manufacturing sites that an active supplier can still reach over operational lanes, avoiding the given nodes and lanes. */
    private static Set<NodeId> suppliedSites(SupplyChainGraph g, Set<NodeId> blockedNodes, Set<EdgeId> blockedLanes) {
        Deque<NodeId> queue = new ArrayDeque<>();
        Set<NodeId> seen = new HashSet<>();
        for (SupplyChainNode n : g.getAllNodes()) {
            if (n.getType() == NodeType.SUPPLIER && n.isActive() && !blockedNodes.contains(n.getId()) && seen.add(n.getId())) queue.add(n.getId());
        }
        while (!queue.isEmpty()) {
            NodeId at = queue.poll();
            for (TransportationLane lane : g.getOutgoingLanes(at)) {
                NodeId to = lane.getTargetNodeId();
                if (!lane.isOperational() || blockedLanes.contains(lane.getId()) || blockedNodes.contains(to) || !g.getNode(to).isActive()) continue;
                if (seen.add(to)) queue.add(to);
            }
        }
        return seen;
    }
}
