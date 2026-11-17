package com.supplygraph.application.quality;

import com.supplygraph.application.port.out.DataQualityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.quality.DataQualityReport;
import com.supplygraph.domain.model.quality.QualityDimension;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Data Quality Engine Evaluation Test")
class DataQualityEngineServiceTest {

    private DataQualityRepositoryPort repository;
    private DataQualityEngineService qualityService;

    @BeforeEach
    void setUp() {
        repository = mock(DataQualityRepositoryPort.class);
        qualityService = new DataQualityEngineService(repository);
    }

    @Test
    @DisplayName("Should evaluate clean digital twin with high quality score")
    void shouldEvaluateCleanState() {
        Supplier s1 = new Supplier(NodeId.of("SUP-01"), "Munich Fab", "Munich, Germany", true, 1, java.util.Set.of("ISO9001"), 0.1);
        Warehouse w1 = new Warehouse(NodeId.of("WH-01"), "Frankfurt Hub", "Frankfurt, Germany", true, BigDecimal.valueOf(10000), false, false);
        TransportationLane lane = new TransportationLane(EdgeId.of("LANE-01"), s1.getId(), w1.getId(), TransportMode.ROAD, Duration.ofDays(2), BigDecimal.valueOf(1.5), true);

        SupplyChainGraph graph = new SupplyChainGraph(List.of(s1, w1), List.of(lane));
        DigitalTwinState state = new DigitalTwinState(
                WorkspaceId.of("WS-CLEAN"),
                StateVersion.initial(),
                graph,
                com.supplygraph.domain.model.bom.BillOfMaterials.empty()
        );

        DataQualityReport report = qualityService.evaluateWorkspaceQuality("WS-CLEAN", state);

        assertThat(report.overallScore()).isGreaterThanOrEqualTo(90.0);
        assertThat(report.findings()).isEmpty();
        verify(repository, times(1)).save(any(DataQualityReport.class));
    }

    @Test
    @DisplayName("Should detect validity violations with penalty deductions")
    void shouldDetectViolationsAndPenalizeScore() {
        // Supplier with blank location triggers VALIDITY finding
        Supplier s1 = new Supplier(NodeId.of("SUP-01"), "Supplier 1", "", true, 1, java.util.Set.of(), 0.5);

        SupplyChainGraph graph = new SupplyChainGraph(List.of(s1), List.of());
        DigitalTwinState state = new DigitalTwinState(
                WorkspaceId.of("WS-FLAWED"),
                StateVersion.initial(),
                graph,
                com.supplygraph.domain.model.bom.BillOfMaterials.empty()
        );

        DataQualityReport report = qualityService.evaluateWorkspaceQuality("WS-FLAWED", state);

        assertThat(report.overallScore()).isLessThan(100.0);
        assertThat(report.findings()).isNotEmpty();
        boolean hasValError = report.findings().stream().anyMatch(f -> f.dimension() == QualityDimension.VALIDITY);
        assertThat(hasValError).isTrue();
    }
}
