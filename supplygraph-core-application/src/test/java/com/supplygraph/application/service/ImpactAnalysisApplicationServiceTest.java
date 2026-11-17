package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.ImpactAssessmentRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.impact.ImpactAssessment;
import com.supplygraph.domain.model.impact.ImpactType;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImpactAnalysisApplicationServiceTest {

    @Test
    @DisplayName("Cascading impact analysis traverses downstream network and explodes BOM shortages")
    void testCascadingImpactAnalysis() {
        WorkspaceId ws = WorkspaceId.of("ws-impact-test");
        DisruptionRepositoryPort disruptionRepo = mock(DisruptionRepositoryPort.class);
        DigitalTwinRepositoryPort twinRepo = mock(DigitalTwinRepositoryPort.class);
        ImpactAssessmentRepositoryPort assessmentRepo = mock(ImpactAssessmentRepositoryPort.class);

        // Build 2-node graph: Supplier -> Manufacturing Site
        NodeId supId = NodeId.of("SUP-CELL");
        NodeId fabId = NodeId.of("FAB-PACK");
        EdgeId laneId = EdgeId.of("LANE-LOGISTICS");

        SupplyChainNode sup = new Supplier(supId, "Battery Cell Supplier", "Korea", true, 1, Set.of(), 0.1);
        SupplyChainNode fab = new ManufacturingSite(fabId, "Pack Assembly Site", "Austin", true, Set.of("LINE-1"), "CLASS-7", 200);
        TransportationLane lane = new TransportationLane(laneId, supId, fabId, TransportMode.AIR, Duration.ofDays(2), BigDecimal.valueOf(5), true);
        SupplyChainGraph graph = new SupplyChainGraph(List.of(sup, fab), List.of(lane));

        // Materials: Cell component
        MaterialId matId = MaterialId.of("BATTERY-CELL-2170");
        Material mat = new Component(matId, "SKU-2170", "Cylindrical Cell", UnitOfMeasure.PIECE, 14);
        BillOfMaterials bom = new BillOfMaterials(List.of(mat), List.of());

        DigitalTwinState twinState = new DigitalTwinState(ws, StateVersion.of(5L), graph, bom);
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(twinState));

        // Create Disruption at Supplier
        Instant now = Instant.now();
        Disruption disruption = Disruption.createDraft(
                ws, "EV_BATTERY", "FIRE", "Fire at Cell Supplier", "Factory fire",
                DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.CRITICAL,
                Set.of(supId), Set.of(laneId), Set.of(matId),
                now, now.plusSeconds(86400 * 7),
                new DisruptionImpactParameters(BigDecimal.valueOf(100), 86400L * 7, 7, BigDecimal.valueOf(10000), "PIECE", Map.of()),
                "Emergency bulletin"
        );
        when(disruptionRepo.findById(disruption.id())).thenReturn(Optional.of(disruption));

        ImpactAnalysisApplicationService service = new ImpactAnalysisApplicationService(
                disruptionRepo, twinRepo, assessmentRepo
        );

        ImpactAssessment assessment = service.performImpactAssessment(disruption.id(), ws);

        assertThat(assessment).isNotNull();
        assertThat(assessment.digitalTwinVersion()).isEqualTo(StateVersion.of(5L));

        // Direct node SUP-CELL
        boolean hasDirect = assessment.nodeImpacts().stream()
                .anyMatch(ni -> ni.nodeId().equals(supId) && ni.impactType() == ImpactType.DIRECT);
        assertThat(hasDirect).isTrue();

        // Cascading downstream node FAB-PACK
        boolean hasIndirect = assessment.nodeImpacts().stream()
                .anyMatch(ni -> ni.nodeId().equals(fabId) && ni.impactType() == ImpactType.INDIRECT);
        assertThat(hasIndirect).isTrue();

        // Shortage computed
        assertThat(assessment.materialShortages()).hasSize(1);
        assertThat(assessment.materialShortages().get(0).shortageQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10000));

        verify(assessmentRepo, times(1)).save(any(ImpactAssessment.class));
    }
}
