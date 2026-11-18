package com.supplygraph.application.sustainability;

import com.supplygraph.application.sustainability.port.out.CarbonEmissionRepositoryPort;
import com.supplygraph.application.sustainability.port.out.CircularLedgerRepositoryPort;
import com.supplygraph.application.sustainability.port.out.EnvironmentalScenarioRepositoryPort;
import com.supplygraph.application.sustainability.port.out.SupplierEsgRepositoryPort;
import com.supplygraph.application.sustainability.port.out.TraceabilityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.TransportMode;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.CircularFlowType;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenario;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.RecallImpactResult;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeId;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SustainabilityApplicationServiceTest {

    private CarbonEmissionRepositoryPort carbonRepo;
    private TraceabilityRepositoryPort traceRepo;
    private SupplierEsgRepositoryPort esgRepo;
    private CircularLedgerRepositoryPort circularRepo;
    private EnvironmentalScenarioRepositoryPort envRepo;

    private CarbonAccountingService carbonService;
    private ProductTraceabilityService traceService;
    private SupplierSustainabilityService esgService;
    private CircularEconomyService circularService;
    private EnvironmentalScenarioService envService;

    private final WorkspaceId workspaceId = new WorkspaceId("ws-sust-app");
    private final NodeId plantBerlin = new NodeId("PLANT-BERLIN");
    private final MaterialId batteryPack = new MaterialId("MAT-BP-4680");

    @BeforeEach
    void setUp() {
        carbonRepo = Mockito.mock(CarbonEmissionRepositoryPort.class);
        traceRepo = Mockito.mock(TraceabilityRepositoryPort.class);
        esgRepo = Mockito.mock(SupplierEsgRepositoryPort.class);
        circularRepo = Mockito.mock(CircularLedgerRepositoryPort.class);
        envRepo = Mockito.mock(EnvironmentalScenarioRepositoryPort.class);

        carbonService = new CarbonAccountingService(carbonRepo);
        traceService = new ProductTraceabilityService(traceRepo);
        esgService = new SupplierSustainabilityService(esgRepo);
        circularService = new CircularEconomyService(circularRepo);
        envService = new EnvironmentalScenarioService(envRepo);
    }

    @Test
    @DisplayName("CarbonAccountingService correctly categorizes Scope 1, 2, and 3 emissions")
    void testCarbonAccountingScopes() {
        CarbonEmissionRecord s1 = carbonService.recordScope1Combustion(workspaceId, plantBerlin, "Natural Gas", 1000.0, "m3", 2.0);
        assertThat(s1.scope()).isEqualTo(EmissionScope.SCOPE_1_DIRECT);
        assertThat(s1.totalEmissionsKgCo2e()).isEqualTo(2000.0);

        CarbonEmissionRecord s2 = carbonService.recordScope2Electricity(workspaceId, plantBerlin, 10000.0, 0.40);
        assertThat(s2.scope()).isEqualTo(EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY);
        assertThat(s2.totalEmissionsKgCo2e()).isEqualTo(4000.0);

        CarbonEmissionRecord s3 = carbonService.recordScope3Freight(workspaceId, new NodeId("LANE-1"), TransportMode.ROAD, 500.0, 20.0);
        assertThat(s3.scope()).isEqualTo(EmissionScope.SCOPE_3_UPSTREAM_FREIGHT);
        assertThat(s3.totalEmissionsKgCo2e()).isEqualTo(620.0);

        when(carbonRepo.findByWorkspaceId(workspaceId)).thenReturn(List.of(s1, s2, s3));

        Map<EmissionScope, Double> totals = carbonService.getEmissionsByScope(workspaceId);
        assertThat(totals.get(EmissionScope.SCOPE_1_DIRECT)).isEqualTo(2.0);
        assertThat(totals.get(EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY)).isEqualTo(4.0);
        assertThat(totals.get(EmissionScope.SCOPE_3_UPSTREAM_FREIGHT)).isEqualTo(0.62);
    }

    @Test
    @DisplayName("ProductTraceabilityService executes forward recall impact analysis")
    void testRecallImpactAnalysis() {
        GenealogyDAG dag = new GenealogyDAG();
        TraceabilityNode tainted = new TraceabilityNode(
                new TraceabilityNodeId("TRC-1"), "BATCH-TAINTED-001", TraceabilityNodeType.SUPPLIER_BATCH,
                batteryPack, plantBerlin, 500.0, "units", "Supplier Inc", Instant.now()
        );
        TraceabilityNode finished = new TraceabilityNode(
                new TraceabilityNodeId("TRC-2"), "LOT-FG-88", TraceabilityNodeType.FINISHED_PRODUCT_LOT,
                batteryPack, plantBerlin, 500.0, "units", "Plant Berlin", Instant.now()
        );
        TraceabilityNode shipment = new TraceabilityNode(
                new TraceabilityNodeId("TRC-3"), "SHIP-ORDER-991", TraceabilityNodeType.SHIPMENT_CONSIGNMENT,
                batteryPack, new NodeId("OEM-CUSTOMER"), 500.0, "units", "Customer Logistics", Instant.now()
        );

        dag.addNode(tainted);
        dag.addNode(finished);
        dag.addNode(shipment);
        dag.addEdge(new com.supplygraph.domain.model.sustainability.GenealogyEdge(tainted.id(), finished.id(), 500.0, "Assembly"));
        dag.addEdge(new com.supplygraph.domain.model.sustainability.GenealogyEdge(finished.id(), shipment.id(), 500.0, "Shipment"));

        when(traceRepo.findDagByWorkspaceId(workspaceId)).thenReturn(Optional.of(dag));

        RecallImpactResult recall = traceService.executeRecallImpactAnalysis(workspaceId, "BATCH-TAINTED-001");
        assertThat(recall.affectedDownstreamNodes()).hasSize(3);
        assertThat(recall.impactedCustomerShipments()).contains("SHIP-ORDER-991");
    }

    @Test
    @DisplayName("SupplierSustainabilityService computes composite ESG and evaluates certifications")
    void testSupplierEsgService() {
        SupplierEsgScorecard card = esgService.recordEsgAudit(
                workspaceId, new NodeId("SUPPLIER-ESG-1"), 80.0, 75.0, 90.0, true, true, true
        );

        assertThat(card.compositeEsgScore()).isEqualTo(81.5);
        assertThat(card.isFullyCompliant()).isTrue();
        verify(esgRepo).save(any());
    }

    @Test
    @DisplayName("CircularEconomyService calculates material recovery yields and virgin material avoidance")
    void testCircularEconomyRecovery() {
        CircularLedgerEntry entry = circularService.recordRecovery(
                workspaceId, batteryPack, CircularFlowType.BATTERY_CELL_RECYCLING, 1000.0, 92.0
        );

        assertThat(entry.recoveredQuantity()).isEqualTo(920.0);
        assertThat(entry.virginMaterialAvoidedKg()).isEqualTo(874.0);
        verify(circularRepo).save(any());
    }

    @Test
    @DisplayName("EnvironmentalScenarioService evaluates decarbonization reduction and abatement cost")
    void testEnvironmentalScenarioSimulation() {
        EnvironmentalScenario scenario = envService.simulateScenario(
                workspaceId, "Intermodal Rail Shift", 1000.0, 750.0, new BigDecimal("15000.00")
        );

        assertThat(scenario.reductionPercentage()).isEqualTo(25.0);
        assertThat(scenario.costPerTonCo2AvoidedUsd()).isEqualTo(new BigDecimal("60.00"));
        verify(envRepo).save(any());
    }
}

