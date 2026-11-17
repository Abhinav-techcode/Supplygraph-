package com.supplygraph.application.intelligence;

import com.supplygraph.application.port.out.ResilienceRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.intelligence.NetworkResilienceAssessment;
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

@DisplayName("Supply Chain Resilience Assessment Service Test")
class SupplyChainResilienceServiceTest {

    @Test
    @DisplayName("Should assess network resilience, calculate HHI, detect SPOF, and produce factor drill-downs")
    void shouldAssessNetworkResilience() {
        var repo = mock(ResilienceRepositoryPort.class);
        var service = new SupplyChainResilienceService(repo);

        Supplier s1 = new Supplier(NodeId.of("SUP-01"), "Sole Source Fab", "Munich, Germany", true, 1, java.util.Set.of("ISO9001"), 0.1);
        ManufacturingSite site1 = new ManufacturingSite(NodeId.of("SITE-01"), "Stuttgart Plant", "Stuttgart, Germany", true, java.util.Set.of("LINE-1"), "ISO-8", 120);
        TransportationLane lane = new TransportationLane(EdgeId.of("L-01"), s1.getId(), site1.getId(), TransportMode.ROAD, Duration.ofDays(16), BigDecimal.valueOf(2.0), true);

        SupplyChainGraph graph = new SupplyChainGraph(List.of(s1, site1), List.of(lane));
        DigitalTwinState state = new DigitalTwinState(
                WorkspaceId.of("WS-RES-01"),
                StateVersion.initial(),
                graph,
                com.supplygraph.domain.model.bom.BillOfMaterials.empty()
        );

        NetworkResilienceAssessment assessment = service.assessNetworkResilience("WS-RES-01", state);

        assertThat(assessment.overallResilienceIndex()).isBetween(0.0, 100.0);
        assertThat(assessment.singlePointsOfFailureCount()).isEqualTo(1);
        assertThat(assessment.supplierConcentrationHHI()).isEqualTo(10000.0); // Monopolistic single-supplier
        assertThat(assessment.factorScores()).hasSize(5);
        assertThat(assessment.criticalVulnerabilities()).isNotEmpty();
        assertThat(assessment.recommendedMitigations()).isNotEmpty();

        verify(repo, times(1)).save(any(NetworkResilienceAssessment.class));
    }
}
