package com.supplygraph.simulation.state;

import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.graph.SupplyChainNode;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Isolated in-memory state representing the virtual digital twin during a discrete-event simulation run.
 * Completely decoupled from the transactional database and authoritative ledger.
 */
public class SimulatedDigitalTwinState {
    private final Map<String, BigDecimal> inventoryBalances = new HashMap<>();
    private final Map<String, Boolean> nodeOperationalStatus = new HashMap<>();
    private final Map<String, Boolean> laneOperationalStatus = new HashMap<>();
    private final Map<String, BigDecimal> nodeCapacityLoss = new HashMap<>();

    private final List<TrajectoryPoint> inventoryTrajectories = new ArrayList<>();
    private final List<TrajectoryPoint> productionTrajectories = new ArrayList<>();
    private final List<TrajectoryPoint> shortageTrajectories = new ArrayList<>();

    public static SimulatedDigitalTwinState fromSnapshot(DigitalTwinSnapshot snapshot) {
        SimulatedDigitalTwinState state = new SimulatedDigitalTwinState();

        // Project nodes
        for (SupplyChainNode node : snapshot.graph().getAllNodes()) {
            state.nodeOperationalStatus.put(node.getId().value(), node.isActive());
            state.nodeCapacityLoss.put(node.getId().value(), BigDecimal.ZERO);
        }

        // Project lanes
        for (TransportationLane lane : snapshot.graph().getAllLanes()) {
            state.laneOperationalStatus.put(lane.getId().value(), lane.isOperational());
        }

        // Project inventory balances
        for (Map.Entry<InventoryAccountId, Quantity> entry : snapshot.inventoryBalances().entrySet()) {
            String key = entry.getKey().nodeId().value() + ":" + entry.getKey().materialId().value();
            state.inventoryBalances.put(key, entry.getValue().amount());
        }

        return state;
    }

    public synchronized BigDecimal getAvailableInventory(String nodeId, String materialId) {
        return inventoryBalances.getOrDefault(nodeId + ":" + materialId, BigDecimal.ZERO);
    }

    public synchronized void adjustInventory(String nodeId, String materialId, BigDecimal delta, Instant timestamp) {
        String key = nodeId + ":" + materialId;
        BigDecimal current = inventoryBalances.getOrDefault(key, BigDecimal.ZERO);
        BigDecimal updated = current.add(delta);
        inventoryBalances.put(key, updated);

        inventoryTrajectories.add(new TrajectoryPoint(timestamp, key, updated, "INVENTORY_AVAILABLE"));
    }

    public synchronized void recordShortage(String nodeId, String materialId, BigDecimal shortageQuantity, Instant timestamp) {
        String key = nodeId + ":" + materialId;
        shortageTrajectories.add(new TrajectoryPoint(timestamp, key, shortageQuantity, "MATERIAL_SHORTAGE"));
    }

    public synchronized void recordProduction(String nodeId, String materialId, BigDecimal quantity, Instant timestamp) {
        String key = nodeId + ":" + materialId;
        productionTrajectories.add(new TrajectoryPoint(timestamp, key, quantity, "PRODUCTION_VOLUME"));
    }

    public synchronized void setNodeOperational(String nodeId, boolean operational) {
        nodeOperationalStatus.put(nodeId, operational);
    }

    public synchronized boolean isNodeOperational(String nodeId) {
        return nodeOperationalStatus.getOrDefault(nodeId, true);
    }

    public synchronized void setLaneOperational(String laneId, boolean operational) {
        laneOperationalStatus.put(laneId, operational);
    }

    public synchronized boolean isLaneOperational(String laneId) {
        return laneOperationalStatus.getOrDefault(laneId, true);
    }

    public synchronized void setNodeCapacityLoss(String nodeId, BigDecimal lossPercentage) {
        nodeCapacityLoss.put(nodeId, lossPercentage);
    }

    public synchronized BigDecimal getNodeCapacityLoss(String nodeId) {
        return nodeCapacityLoss.getOrDefault(nodeId, BigDecimal.ZERO);
    }

    public synchronized List<TrajectoryPoint> getInventoryTrajectories() {
        return Collections.unmodifiableList(new ArrayList<>(inventoryTrajectories));
    }

    public synchronized List<TrajectoryPoint> getProductionTrajectories() {
        return Collections.unmodifiableList(new ArrayList<>(productionTrajectories));
    }

    public synchronized List<TrajectoryPoint> getShortageTrajectories() {
        return Collections.unmodifiableList(new ArrayList<>(shortageTrajectories));
    }

    public synchronized Map<String, BigDecimal> getAllCurrentBalances() {
        return Collections.unmodifiableMap(new HashMap<>(inventoryBalances));
    }
}

