package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanningDomainTest {

    private final WorkspaceId workspaceId = new WorkspaceId("ws-plan-test");
    private final MaterialId batteryPack = new MaterialId("MAT-BP-4680");
    private final NodeId plantNode = new NodeId("PLANT-BERLIN");

    @Test
    @DisplayName("DemandForecast correctly applies period overrides and aggregates volume")
    void testDemandForecastCalculations() {
        DemandForecast forecast = new DemandForecast(
                DemandForecastId.generate(),
                workspaceId,
                batteryPack,
                plantNode,
                ForecastMethod.EXPONENTIAL_SMOOTHING,
                List.of(100.0, 110.0, 120.0),
                List.of(new HistoricalDemandPoint(LocalDate.now().minusMonths(1), 95.0)),
                new ForecastAccuracyMetrics(5.0, 0.04, 6.2, 0.5),
                Map.of(1, 150.0), // Override second period from 110 to 150
                Instant.now()
        );

        assertThat(forecast.getEffectiveQuantity(0)).isEqualTo(100.0);
        assertThat(forecast.getEffectiveQuantity(1)).isEqualTo(150.0);
        assertThat(forecast.getEffectiveQuantity(2)).isEqualTo(120.0);
        assertThat(forecast.getTotalForecastedVolume()).isEqualTo(370.0);
    }

    @Test
    @DisplayName("EchelonInventoryPosition flags reorder condition when position <= reorderPoint")
    void testEchelonInventoryPositionReorderFlag() {
        EchelonInventoryPosition position = new EchelonInventoryPosition(
                plantNode,
                EchelonTier.PLANT_WAREHOUSE,
                batteryPack,
                40.0,
                10.0,
                30.0,
                60.0,
                200.0,
                2.50
        );

        assertThat(position.getInventoryPosition()).isEqualTo(50.0);
        assertThat(position.isReorderRequired()).isTrue();
        assertThat(position.calculateAnnualCarryingCost()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("SafetyStockResult validates non-negative parameters")
    void testSafetyStockResultInvariants() {
        assertThatThrownBy(() -> new SafetyStockResult(-10.0, 5.0, 5.0, -1.0, 50.0, 100.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ScheduledProductionJob correctly calculates total occupied hours")
    void testScheduledProductionJobOccupancy() {
        ScheduledProductionJob job = new ScheduledProductionJob(
                "JOB-101",
                batteryPack,
                plantNode,
                LocalDate.now(),
                1,
                500.0,
                1.5,
                6.5,
                1
        );

        assertThat(job.getTotalOccupiedHours()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("PlanningScenarioComparison enforces non-null fields and approval check")
    void testPlanningScenarioComparison() {
        PlanningScenarioComparison comparison = new PlanningScenarioComparison(
                PlanningScenarioId.generate(),
                workspaceId,
                "Surge Buffer Strategy",
                PlanningScenarioType.DEMAND_SURGE,
                PlanningPlanStatus.APPROVED,
                new BigDecimal("1500000.00"),
                98.5,
                4.2,
                125.0,
                "lead-planner",
                Instant.now()
        );

        assertThat(comparison.isApproved()).isTrue();
    }
}
