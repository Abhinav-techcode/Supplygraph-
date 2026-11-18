package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.PostgresDemandForecastPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresInventoryPlanPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresPlanningScenarioPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresProcurementPlanPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresProductionSchedulePersistenceAdapter;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.DemandForecastId;
import com.supplygraph.domain.model.planning.EchelonInventoryPosition;
import com.supplygraph.domain.model.planning.EchelonTier;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.domain.model.planning.ForecastAccuracyMetrics;
import com.supplygraph.domain.model.planning.ForecastMethod;
import com.supplygraph.domain.model.planning.HistoricalDemandPoint;
import com.supplygraph.domain.model.planning.InventoryPlan;
import com.supplygraph.domain.model.planning.InventoryPlanId;
import com.supplygraph.domain.model.planning.NetRequirementItem;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.planning.PlanningScenarioId;
import com.supplygraph.domain.model.planning.PlanningScenarioType;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.domain.model.planning.ProcurementPlanId;
import com.supplygraph.domain.model.planning.ProductionScheduleId;
import com.supplygraph.domain.model.planning.ScheduledProductionJob;
import com.supplygraph.domain.model.planning.ServiceLevel;
import com.supplygraph.domain.model.planning.SupplierAllocationShare;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase11PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 11 Planning PostgreSQL Persistence Integration Test")
class Phase11PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_p11_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private PostgresDemandForecastPersistenceAdapter forecastAdapter;
    @Autowired private PostgresInventoryPlanPersistenceAdapter inventoryAdapter;
    @Autowired private PostgresProcurementPlanPersistenceAdapter procurementAdapter;
    @Autowired private PostgresProductionSchedulePersistenceAdapter productionAdapter;
    @Autowired private PostgresPlanningScenarioPersistenceAdapter scenarioAdapter;

    private final WorkspaceId workspaceId = new WorkspaceId("ws-p11-test");
    private final MaterialId batteryPack = new MaterialId("MAT-BP-4680");
    private final NodeId plantNode = new NodeId("PLANT-BERLIN");

    @Test
    @DisplayName("Demand forecast persists and reloads with overrides and accuracy metrics")
    void testDemandForecastPersistence() {
        DemandForecast forecast = new DemandForecast(
                DemandForecastId.generate(),
                workspaceId,
                batteryPack,
                plantNode,
                ForecastMethod.EXPONENTIAL_SMOOTHING,
                List.of(120.0, 130.0, 140.0),
                List.of(new HistoricalDemandPoint(LocalDate.now().minusMonths(1), 115.0)),
                new ForecastAccuracyMetrics(4.5, 0.038, 5.2, 0.1),
                Map.of(0, 125.0),
                Instant.now()
        );

        forecastAdapter.save(forecast);

        Optional<DemandForecast> loaded = forecastAdapter.findById(forecast.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getEffectiveQuantity(0)).isEqualTo(125.0);
        assertThat(loaded.get().method()).isEqualTo(ForecastMethod.EXPONENTIAL_SMOOTHING);
        assertThat(loaded.get().accuracyMetrics().meanAbsoluteDeviation()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("Multi-echelon inventory plan persists and reloads positions")
    void testInventoryPlanPersistence() {
        EchelonInventoryPosition pos = new EchelonInventoryPosition(
                plantNode, EchelonTier.PLANT_WAREHOUSE, batteryPack, 150.0, 50.0, 40.0, 80.0, 300.0, 3.20
        );

        InventoryPlan plan = new InventoryPlan(
                InventoryPlanId.generate(),
                workspaceId,
                "Q1 Network Stock Plan",
                ServiceLevel.SL_95,
                List.of(pos),
                480.0,
                Instant.now()
        );

        inventoryAdapter.save(plan);

        Optional<InventoryPlan> loaded = inventoryAdapter.findById(plan.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().positions()).hasSize(1);
        assertThat(loaded.get().targetServiceLevel()).isEqualTo(ServiceLevel.SL_95);
    }

    @Test
    @DisplayName("Procurement plan persists net requirements and multi-supplier allocations")
    void testProcurementPlanPersistence() {
        NetRequirementItem req = new NetRequirementItem(batteryPack, plantNode, 500.0, 100.0, 50.0, 350.0, LocalDate.now().plusWeeks(2));
        SupplierAllocationShare share = new SupplierAllocationShare(
                new NodeId("SUPPLIER-ALPHA"), batteryPack, 100.0, 350.0, new BigDecimal("120.00"), new BigDecimal("42000.00")
        );

        ProcurementPlan plan = new ProcurementPlan(
                ProcurementPlanId.generate(),
                workspaceId,
                "Battery Sourcing Plan",
                List.of(req),
                List.of(share),
                new BigDecimal("42000.00"),
                Instant.now()
        );

        procurementAdapter.save(plan);

        Optional<ProcurementPlan> loaded = procurementAdapter.findById(plan.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().supplierAllocations()).hasSize(1);
        assertThat(loaded.get().totalProjectedCost()).isEqualByComparingTo("42000.00");
    }

    @Test
    @DisplayName("Production schedule persists finite jobs and capacity utilization")
    void testProductionSchedulePersistence() {
        ScheduledProductionJob job = new ScheduledProductionJob("JOB-P11-1", batteryPack, plantNode, LocalDate.now(), 1, 200.0, 1.5, 6.0, 1);
        FiniteProductionSchedule schedule = new FiniteProductionSchedule(
                ProductionScheduleId.generate(),
                workspaceId,
                plantNode,
                "Cell Line A Schedule",
                List.of(job),
                0.9375,
                Instant.now()
        );

        productionAdapter.save(schedule);

        Optional<FiniteProductionSchedule> loaded = productionAdapter.findById(schedule.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().scheduledJobs()).hasSize(1);
        assertThat(loaded.get().averageCapacityUtilization()).isEqualTo(0.9375);
    }

    @Test
    @DisplayName("Planning scenario comparison persists and filters by workspace")
    void testPlanningScenarioPersistence() {
        PlanningScenarioComparison scenario = new PlanningScenarioComparison(
                PlanningScenarioId.generate(),
                workspaceId,
                "Accelerated Freight Scenario",
                PlanningScenarioType.ACCELERATED_DELIVERY,
                PlanningPlanStatus.APPROVED,
                new BigDecimal("2100000.00"),
                99.2,
                2.8,
                340.0,
                "vp-supply-chain",
                Instant.now()
        );

        scenarioAdapter.save(scenario);

        Optional<PlanningScenarioComparison> loaded = scenarioAdapter.findById(scenario.scenarioId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().scenarioType()).isEqualTo(PlanningScenarioType.ACCELERATED_DELIVERY);
        assertThat(loaded.get().isApproved()).isTrue();
    }
}
