package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SustainabilityDomainTest {

    private final WorkspaceId workspaceId = new WorkspaceId("ws-sust-test");
    private final NodeId plantBerlin = new NodeId("PLANT-BERLIN");
    private final MaterialId lithiumMaterial = new MaterialId("MAT-LITHIUM-HYDROXIDE");
    private final MaterialId batteryPack = new MaterialId("MAT-BP-4680");

    @Test
    @DisplayName("CarbonEmissionRecord converts kg to metric tons correctly")
    void testCarbonRecordTonConversion() {
        CarbonEmissionRecord record = new CarbonEmissionRecord(
                CarbonEmissionRecordId.generate(),
                workspaceId,
                plantBerlin,
                EmissionScope.SCOPE_1_DIRECT,
                "Natural Gas Plant Heating",
                5000.0,
                "m3",
                2.02,
                10100.0,
                Instant.now()
        );

        assertThat(record.getTotalEmissionsTons()).isEqualTo(10.1);
    }

    @Test
    @DisplayName("GenealogyDAG performs bidirectional lot tracing (backward to mine, forward to recall)")
    void testGenealogyDagTraversals() {
        GenealogyDAG dag = new GenealogyDAG();

        TraceabilityNode mineOrigin = new TraceabilityNode(
                new TraceabilityNodeId("NODE-MINE-1"), "BATCH-MINE-LITHIUM-01", TraceabilityNodeType.RAW_MATERIAL_ORIGIN,
                lithiumMaterial, new NodeId("MINE-CHILE"), 10000.0, "kg", "Atacama Mining Co", Instant.now().minusSeconds(86400 * 30)
        );

        TraceabilityNode supplierRefined = new TraceabilityNode(
                new TraceabilityNodeId("NODE-SUPP-1"), "BATCH-REFINED-LI-88", TraceabilityNodeType.SUPPLIER_BATCH,
                lithiumMaterial, new NodeId("SUPPLIER-REFINERY"), 9800.0, "kg", "Apex Chemical Corp", Instant.now().minusSeconds(86400 * 20)
        );

        TraceabilityNode cellAssembly = new TraceabilityNode(
                new TraceabilityNodeId("NODE-CELL-1"), "BATCH-CELL-LINE-A-501", TraceabilityNodeType.WORK_ORDER_BATCH,
                new MaterialId("MAT-CELL-4680"), plantBerlin, 5000.0, "units", "Berlin Gigafactory", Instant.now().minusSeconds(86400 * 10)
        );

        TraceabilityNode finishedPack = new TraceabilityNode(
                new TraceabilityNodeId("NODE-PACK-1"), "LOT-PACK-EV-901", TraceabilityNodeType.FINISHED_PRODUCT_LOT,
                batteryPack, plantBerlin, 200.0, "units", "Berlin Pack Assembly", Instant.now().minusSeconds(86400 * 5)
        );

        TraceabilityNode customerShipment = new TraceabilityNode(
                new TraceabilityNodeId("NODE-SHIP-1"), "DISPATCH-CONSIGNMENT-302", TraceabilityNodeType.SHIPMENT_CONSIGNMENT,
                batteryPack, new NodeId("OEM-MUNICH"), 50.0, "units", "DHL Freight Europe", Instant.now()
        );

        dag.addNode(mineOrigin);
        dag.addNode(supplierRefined);
        dag.addNode(cellAssembly);
        dag.addNode(finishedPack);
        dag.addNode(customerShipment);

        dag.addEdge(new GenealogyEdge(mineOrigin.id(), supplierRefined.id(), 9800.0, "Hydroxide Refining"));
        dag.addEdge(new GenealogyEdge(supplierRefined.id(), cellAssembly.id(), 4500.0, "Cell Cathode Coating"));
        dag.addEdge(new GenealogyEdge(cellAssembly.id(), finishedPack.id(), 200.0, "Pack Module Assembly"));
        dag.addEdge(new GenealogyEdge(finishedPack.id(), customerShipment.id(), 50.0, "OEM Delivery"));

        // Backward trace from finished pack should trace back to origin mine
        List<TraceabilityNode> backwardTrace = dag.backwardTrace(finishedPack.id());
        assertThat(backwardTrace).extracting(TraceabilityNode::id)
                .containsExactlyInAnyOrder(finishedPack.id(), cellAssembly.id(), supplierRefined.id(), mineOrigin.id());

        // Forward trace from tainted supplier batch should find cell, pack, and customer shipment
        List<TraceabilityNode> forwardTrace = dag.forwardTrace(supplierRefined.id());
        assertThat(forwardTrace).extracting(TraceabilityNode::id)
                .containsExactlyInAnyOrder(supplierRefined.id(), cellAssembly.id(), finishedPack.id(), customerShipment.id());
    }

    @Test
    @DisplayName("SupplierEsgScorecard correctly validates compliance criteria")
    void testSupplierEsgCompliance() {
        SupplierEsgScorecard compliant = new SupplierEsgScorecard(
                EsgScorecardId.generate(),
                workspaceId,
                new NodeId("SUPPLIER-ALPHA"),
                82.0,
                88.0,
                85.0,
                85.0,
                true,
                true,
                true,
                Instant.now()
        );

        assertThat(compliant.isFullyCompliant()).isTrue();

        SupplierEsgScorecard nonCompliant = new SupplierEsgScorecard(
                EsgScorecardId.generate(),
                workspaceId,
                new NodeId("SUPPLIER-BETA"),
                55.0,
                60.0,
                65.0,
                60.0,
                false, // Fails CBAM
                true,
                true,
                Instant.now()
        );

        assertThat(nonCompliant.isFullyCompliant()).isFalse();
    }
}

