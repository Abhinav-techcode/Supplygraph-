package com.supplygraph.domain.model.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplyChainGraphTest {

    @Test
    @DisplayName("Graph builds adjacency lists and validates endpoints correctly")
    void shouldConstructValidGraph() {
        Supplier tier1Supplier = new Supplier(
                NodeId.of("SUP-01"), "Lithium Refining Corp", "Chile", true, 1, Set.of("ISO9001"), 0.15);
        ManufacturingSite cellPlant = new ManufacturingSite(
                NodeId.of("MFG-01"), "Gigafactory Nevada", "USA", true, Set.of("LINE-A"), "ISO-7", 500);
        Warehouse centralWh = new Warehouse(
                NodeId.of("WH-01"), "Central Distribution Hub", "USA", true, new BigDecimal("25000"), true, false);

        TransportationLane lane1 = new TransportationLane(
                EdgeId.of("LANE-01"), NodeId.of("SUP-01"), NodeId.of("MFG-01"),
                TransportMode.OCEAN, Duration.ofDays(14), new BigDecimal("1.85"), true);
        TransportationLane lane2 = new TransportationLane(
                EdgeId.of("LANE-02"), NodeId.of("MFG-01"), NodeId.of("WH-01"),
                TransportMode.ROAD, Duration.ofDays(2), new BigDecimal("0.45"), true);

        SupplyChainGraph graph = new SupplyChainGraph(List.of(tier1Supplier, cellPlant, centralWh), List.of(lane1, lane2));

        assertThat(graph.getNodeCount()).isEqualTo(3);
        assertThat(graph.getLaneCount()).isEqualTo(2);

        assertThat(graph.getDirectPredecessors(NodeId.of("MFG-01"))).containsExactly(NodeId.of("SUP-01"));
        assertThat(graph.getDirectSuccessors(NodeId.of("MFG-01"))).containsExactly(NodeId.of("WH-01"));

        Set<NodeId> upstreamFromWh = graph.getUpstreamReachableNodes(NodeId.of("WH-01"));
        assertThat(upstreamFromWh).containsExactlyInAnyOrder(NodeId.of("MFG-01"), NodeId.of("SUP-01"));

        Set<NodeId> downstreamFromSupplier = graph.getDownstreamReachableNodes(NodeId.of("SUP-01"));
        assertThat(downstreamFromSupplier).containsExactlyInAnyOrder(NodeId.of("MFG-01"), NodeId.of("WH-01"));

        List<List<TransportationLane>> paths = graph.findAllPaths(NodeId.of("SUP-01"), NodeId.of("WH-01"));
        assertThat(paths).hasSize(1);
        assertThat(paths.get(0)).containsExactly(lane1, lane2);
    }

    @Test
    @DisplayName("Graph rejects lanes connecting non-existent nodes")
    void shouldRejectDanglingLanes() {
        Supplier supplier = new Supplier(
                NodeId.of("SUP-01"), "Lithium Supplier", "Chile", true, 1, Set.of(), 0.1);
        TransportationLane invalidLane = new TransportationLane(
                EdgeId.of("LANE-ERR"), NodeId.of("SUP-01"), NodeId.of("NON-EXISTENT"),
                TransportMode.AIR, Duration.ofDays(1), BigDecimal.ONE, true);

        assertThatThrownBy(() -> new SupplyChainGraph(List.of(supplier), List.of(invalidLane)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lane target node does not exist in graph");
    }

    @Test
    @DisplayName("Lanes reject self-loops")
    void shouldRejectSelfLoopLanes() {
        assertThatThrownBy(() -> new TransportationLane(
                EdgeId.of("LOOP"), NodeId.of("N1"), NodeId.of("N1"),
                TransportMode.ROAD, Duration.ofHours(1), BigDecimal.TEN, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source and target nodes cannot be identical");
    }
}

