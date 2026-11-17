package com.supplygraph.simulation.engine;

import com.supplygraph.domain.model.bom.BomEntry;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.simulation.api.SimulationClock;
import com.supplygraph.simulation.event.SimulationEvent;
import com.supplygraph.simulation.event.SimulationEventQueue;
import com.supplygraph.simulation.event.SimulationEventType;
import com.supplygraph.simulation.state.SimulatedDigitalTwinState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic discrete-event simulation engine for supply networks.
 * Runs in complete memory isolation from the authoritative digital twin and inventory ledger.
 */
public class DiscreteEventSimulationEngine {

    public SimulationResult run(
            DigitalTwinSnapshot snapshot,
            Disruption disruption,
            RecoveryPlan recoveryPlan,
            SimulationConfiguration config
    ) {
        Objects.requireNonNull(snapshot, "DigitalTwinSnapshot must not be null");
        Objects.requireNonNull(disruption, "Disruption must not be null");
        Objects.requireNonNull(config, "SimulationConfiguration must not be null");

        String simId = "SIM-" + UUID.randomUUID().toString().substring(0, 8);
        SimulationClock clock = SimulationClock.initial(config.startTime());
        SimulatedDigitalTwinState state = SimulatedDigitalTwinState.fromSnapshot(snapshot);
        SimulationEventQueue queue = new SimulationEventQueue();

        List<SimulationEvent> processedEvents = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();
        assumptions.add("Deterministic lead-time and fixed demand profile applied over horizon");

        // 1. Enqueue initial disruption event
        Instant disruptionStart = disruption.startTime().isBefore(config.startTime()) ? config.startTime() : disruption.startTime();
        queue.enqueue(new SimulationEvent(
                "EVT-DISRUPT-START",
                disruptionStart,
                0,
                SimulationEventType.DISRUPTION_BEGINS,
                disruption.id().value(),
                disruption.title(),
                Map.of("category", disruption.category().name(), "severity", disruption.severity().name())
        ), clock.currentTime());

        // Enqueue disruption resolution if within horizon
        if (disruption.expectedEndTime() != null && disruption.expectedEndTime().isBefore(config.endTime())) {
            queue.enqueue(new SimulationEvent(
                    "EVT-DISRUPT-RESOLVE",
                    disruption.expectedEndTime(),
                    1,
                    SimulationEventType.DISRUPTION_RESOLVES,
                    disruption.id().value(),
                    "Disruption expected resolution",
                    Map.of()
            ), clock.currentTime());
        }

        // 2. Enqueue recovery actions if provided
        if (config.includeRecoveryPlan() && recoveryPlan != null) {
            int actionIdx = 0;
            for (PlanAction action : recoveryPlan.actions()) {
                Instant actionTime = disruptionStart.plusSeconds(action.estimatedDurationHours() * 3600L);
                if (actionTime.isBefore(config.endTime())) {
                    queue.enqueue(new SimulationEvent(
                            "EVT-RECOVERY-" + (++actionIdx),
                            actionTime,
                            2,
                            SimulationEventType.RECOVERY_ACTION_TAKES_EFFECT,
                            action.targetNodeId() != null ? action.targetNodeId().value() : "GLOBAL",
                            action.description(),
                            Map.of("actionType", action.actionType().name(), "quantity", action.quantity())
                    ), clock.currentTime());
                }
            }
        }

        // 3. Schedule periodic production and shipment check events across time steps
        Instant cursor = config.startTime();
        long step = Math.max(3600L, config.stepSeconds());
        int tick = 0;
        while (!cursor.isAfter(config.endTime()) && tick < 1000) {
            cursor = cursor.plusSeconds(step);
            tick++;

            // For each operational lane, schedule delivery arrivals
            for (TransportationLane lane : snapshot.graph().getAllLanes()) {
                if (lane.isOperational()) {
                    queue.enqueue(new SimulationEvent(
                            "EVT-LANE-DELIVERY-" + tick + "-" + lane.getId().value(),
                            cursor,
                            5,
                            SimulationEventType.SUPPLIER_DELIVERY_ARRIVES,
                            lane.getId().value(),
                            "Scheduled lane transit arrival",
                            Map.of("laneId", lane.getId().value())
                    ), clock.currentTime());
                }
            }
        }

        // 4. Main Event Processing Loop
        BigDecimal totalShortage = BigDecimal.ZERO;
        long totalDemand = 0;
        long totalFulfilled = 0;
        long maxDelaySeconds = 0;

        while (!queue.isEmpty()) {
            if (processedEvents.size() >= config.maxEvents()) {
                warnings.add("Simulation reached maximum event limit safeguard: " + config.maxEvents());
                break;
            }

            Optional<SimulationEvent> nextOpt = queue.pollNext();
            if (nextOpt.isEmpty()) {
                break;
            }

            SimulationEvent event = nextOpt.get();
            if (event.timestamp().isAfter(config.endTime())) {
                break;
            }

            // Monotonically advance clock
            if (event.timestamp().isAfter(clock.currentTime())) {
                clock = clock.advanceTo(event.timestamp());
            }

            // Process event
            switch (event.eventType()) {
                case DISRUPTION_BEGINS -> {
                    for (NodeId nid : disruption.affectedNodeIds()) {
                        state.setNodeOperational(nid.value(), false);
                        BigDecimal loss = disruption.impactParameters().capacityLossPercentage();
                        state.setNodeCapacityLoss(nid.value(), loss != null ? loss : BigDecimal.valueOf(100));
                    }
                    for (EdgeId eid : disruption.affectedLaneIds()) {
                        state.setLaneOperational(eid.value(), false);
                    }
                    for (MaterialId mid : disruption.affectedMaterialIds()) {
                        // Mark material shortage observation
                        state.recordShortage("DISRUPTED_NODE", mid.value(), disruption.impactParameters().affectedQuantity(), clock.currentTime());
                    }
                }
                case DISRUPTION_RESOLVES -> {
                    for (NodeId nid : disruption.affectedNodeIds()) {
                        state.setNodeOperational(nid.value(), true);
                        state.setNodeCapacityLoss(nid.value(), BigDecimal.ZERO);
                    }
                    for (EdgeId eid : disruption.affectedLaneIds()) {
                        state.setLaneOperational(eid.value(), true);
                    }
                }
                case SUPPLIER_DELIVERY_ARRIVES -> {
                    String laneId = (String) event.payload().get("laneId");
                    if (laneId != null && state.isLaneOperational(laneId)) {
                        TransportationLane lane = snapshot.graph().findLane(EdgeId.of(laneId)).orElse(null);
                        if (lane != null) {
                            // Replenish target node
                            state.adjustInventory(lane.getTargetNodeId().value(), "MATERIAL_STD", BigDecimal.valueOf(50), clock.currentTime());
                        }
                    }
                }
                case RECOVERY_ACTION_TAKES_EFFECT -> {
                    String qtyStr = String.valueOf(event.payload().getOrDefault("quantity", "0"));
                    BigDecimal qty = new BigDecimal(qtyStr);
                    state.adjustInventory(event.entityId(), "MATERIAL_STD", qty, clock.currentTime());
                    state.setNodeOperational(event.entityId(), true);
                }
                case MATERIAL_SHORTAGE_OCCURS -> {
                    totalShortage = totalShortage.add(BigDecimal.TEN);
                }
                default -> {
                    // Non-state mutating events logged
                }
            }

            processedEvents.add(event);
        }

        // Compute service level and shortage outcomes
        for (var point : state.getShortageTrajectories()) {
            totalShortage = totalShortage.add(point.value());
        }

        if (disruption.impactParameters().expectedDelaySeconds() != null) {
            maxDelaySeconds = disruption.impactParameters().expectedDelaySeconds();
        }

        double serviceLevel = totalShortage.compareTo(BigDecimal.ZERO) == 0 ? 100.0 : Math.max(40.0, 100.0 - (totalShortage.doubleValue() / 100.0));

        return new SimulationResult(
                simId,
                disruption.scenarioType(),
                snapshot.version(),
                config.startTime(),
                clock.currentTime(),
                processedEvents.size(),
                processedEvents,
                state.getInventoryTrajectories(),
                state.getProductionTrajectories(),
                state.getShortageTrajectories(),
                serviceLevel,
                totalShortage,
                maxDelaySeconds,
                "COMPLETED",
                warnings,
                assumptions
        );
    }
}

