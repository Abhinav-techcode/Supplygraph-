package com.supplygraph.application.intelligence;

import com.supplygraph.application.port.out.MaterialIntelligenceRepositoryPort;
import com.supplygraph.application.port.out.SupplierIntelligenceRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.intelligence.MaterialCriticalityLevel;
import com.supplygraph.domain.model.intelligence.MaterialIntelligenceProfile;
import com.supplygraph.domain.model.intelligence.SupplierRiskProfile;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Supplier and Material Intelligence Service Tests")
class SupplierAndMaterialIntelligenceTest {

    @Test
    @DisplayName("Should compute supplier risk profiles, OTIF reliability, and single source indicators")
    void shouldComputeSupplierRiskProfiles() {
        var supplierRepo = mock(SupplierIntelligenceRepositoryPort.class);
        var service = new SupplierIntelligenceService(supplierRepo);

        Supplier s1 = new Supplier(NodeId.of("SUP-01"), "Munich Fab", "Munich, Germany", true, 1, java.util.Set.of("ISO9001"), 0.1);
        ManufacturingSite site1 = new ManufacturingSite(NodeId.of("SITE-01"), "Stuttgart Assembly", "Stuttgart, Germany", true, java.util.Set.of("LINE-1"), "ISO-8", 120);
        TransportationLane lane = new TransportationLane(EdgeId.of("L-01"), s1.getId(), site1.getId(), TransportMode.ROAD, Duration.ofDays(2), BigDecimal.valueOf(2.0), true);

        SupplyChainGraph graph = new SupplyChainGraph(List.of(s1, site1), List.of(lane));
        DigitalTwinState state = new DigitalTwinState(
                WorkspaceId.of("WS-01"),
                StateVersion.initial(),
                graph,
                BillOfMaterials.empty()
        );

        List<SupplierRiskProfile> profiles = service.computeSupplierRiskProfiles("WS-01", state);

        assertThat(profiles).hasSize(1);
        SupplierRiskProfile p = profiles.get(0);
        assertThat(p.supplierId()).isEqualTo("SUP-01");
        assertThat(p.country()).isEqualTo("Munich, Germany");
        assertThat(p.overallRiskScore()).isBetween(0.0, 1.0);
        assertThat(p.metrics()).isNotEmpty();
        verify(supplierRepo, times(1)).saveAll(eq("WS-01"), any());
    }

    @Test
    @DisplayName("Should compute material intelligence profiles, runway days, and lead time stockout flags")
    void shouldComputeMaterialIntelligenceProfiles() {
        var materialRepo = mock(MaterialIntelligenceRepositoryPort.class);
        var service = new MaterialIntelligenceService(materialRepo);

        Material mcu = new Material(MaterialId.of("MAT-MCU-32"), "SKU-MCU", "Automotive 32-bit MCU", MaterialType.COMPONENT, UnitOfMeasure.PIECE);
        BillOfMaterials bom = new BillOfMaterials(List.of(mcu), List.of());

        DigitalTwinState state = new DigitalTwinState(
                WorkspaceId.of("WS-02"),
                StateVersion.initial(),
                SupplyChainGraph.empty(),
                bom
        );

        List<MaterialIntelligenceProfile> profiles = service.computeMaterialProfiles("WS-02", state, BigDecimal.valueOf(20));

        assertThat(profiles).hasSize(1);
        MaterialIntelligenceProfile p = profiles.get(0);
        assertThat(p.materialId()).isEqualTo("MAT-MCU-32");
        assertThat(p.criticality()).isEqualTo(MaterialCriticalityLevel.VERY_HIGH);
        assertThat(p.inventoryRunwayDays()).isGreaterThan(BigDecimal.ZERO);
        verify(materialRepo, times(1)).saveAll(eq("WS-02"), any());
    }
}
