package com.supplygraph.application.planning;

import com.supplygraph.domain.error.AccessDeniedException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.planning.port.out.DemandForecastRepositoryPort;
import com.supplygraph.application.planning.port.out.InventoryPlanRepositoryPort;
import com.supplygraph.application.planning.port.out.PlanningScenarioRepositoryPort;
import com.supplygraph.application.planning.port.out.ProcurementPlanRepositoryPort;
import com.supplygraph.application.planning.port.out.ProductionScheduleRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.DemandForecastId;
import com.supplygraph.domain.model.planning.EchelonInventoryPosition;
import com.supplygraph.domain.model.planning.EchelonTier;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.domain.model.planning.ForecastMethod;
import com.supplygraph.domain.model.planning.HistoricalDemandPoint;
import com.supplygraph.domain.model.planning.InventoryPlan;
import com.supplygraph.domain.model.planning.NetRequirementItem;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.planning.PlanningScenarioId;
import com.supplygraph.domain.model.planning.PlanningScenarioType;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.domain.model.planning.SafetyStockResult;
import com.supplygraph.domain.model.planning.ScheduledProductionJob;
import com.supplygraph.domain.model.planning.ServiceLevel;
import com.supplygraph.domain.model.planning.SupplierAllocationShare;
import com.supplygraph.domain.model.planning.TransportModeTradeoff;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanningApplicationServiceTest {

    private DemandForecastRepositoryPort forecastRepo;
    private InventoryPlanRepositoryPort inventoryRepo;
    private ProcurementPlanRepositoryPort procurementRepo;
    private ProductionScheduleRepositoryPort productionRepo;
    private PlanningScenarioRepositoryPort scenarioRepo;
    private AuditGovernanceService auditService;

    private DemandPlanningService demandService;
    private MultiEchelonInventoryService inventoryService;
    private ProcurementPlanningService procurementService;
    private ProductionPlanningService productionService;
    private SupplyChainNetworkPlanningService networkService;
    private PlanningScenarioService scenarioService;

    private final WorkspaceId workspaceId = new WorkspaceId("ws-plan-app");
    private final MaterialId batteryPack = new MaterialId("MAT-BP-4680");
    private final NodeId plantNode = new NodeId("PLANT-BERLIN");

    @BeforeEach
    void setUp() {
        forecastRepo = Mockito.mock(DemandForecastRepositoryPort.class);
        inventoryRepo = Mockito.mock(InventoryPlanRepositoryPort.class);
        procurementRepo = Mockito.mock(ProcurementPlanRepositoryPort.class);
        productionRepo = Mockito.mock(ProductionScheduleRepositoryPort.class);
        scenarioRepo = Mockito.mock(PlanningScenarioRepositoryPort.class);
        auditService = Mockito.mock(AuditGovernanceService.class);

        demandService = new DemandPlanningService(forecastRepo);
        inventoryService = new MultiEchelonInventoryService(inventoryRepo);
        procurementService = new ProcurementPlanningService(procurementRepo);
        productionService = new ProductionPlanningService(productionRepo);
        networkService = new SupplyChainNetworkPlanningService();
        scenarioService = new PlanningScenarioService(scenarioRepo, auditService);
    }

    @Test
    @DisplayName("DemandPlanningService generates SMA, WMA, and Holt-Winters forecasts")
    void testDemandForecastingMethods() {
        List<HistoricalDemandPoint> history = List.of(
                new HistoricalDemandPoint(LocalDate.now().minusMonths(3), 100.0),
                new HistoricalDemandPoint(LocalDate.now().minusMonths(2), 110.0),
                new HistoricalDemandPoint(LocalDate.now().minusMonths(1), 120.0)
        );

        DemandForecast sma = demandService.generateForecast(workspaceId, batteryPack, plantNode, ForecastMethod.SIMPLE_MOVING_AVERAGE, history, 3, 0.3, 0.1);
        assertThat(sma.forecastedQuantities()).hasSize(3);
        assertThat(sma.forecastedQuantities().get(0)).isEqualTo(110.0);

        DemandForecast wma = demandService.generateForecast(workspaceId, batteryPack, plantNode, ForecastMethod.WEIGHTED_MOVING_AVERAGE, history, 3, 0.3, 0.1);
        assertThat(wma.forecastedQuantities().get(0)).isGreaterThan(110.0);

        DemandForecast hw = demandService.generateForecast(workspaceId, batteryPack, plantNode, ForecastMethod.TREND_ADJUSTED_HOLT_WINTERS, history, 3, 0.3, 0.1);
        assertThat(hw.forecastedQuantities()).hasSize(3);
    }

    @Test
    @DisplayName("MultiEchelonInventoryService computes safety stock and EOQ correctly")
    void testSafetyStockAndEoqCalculations() {
        SafetyStockResult result = inventoryService.calculateSafetyStock(
                50.0, // mean daily demand
                10.0, // std dev daily demand
                7,    // lead time days
                1.5,  // std dev lead time
                ServiceLevel.SL_95,
                5.0,  // annual holding cost
                120.0 // ordering setup cost
        );

        assertThat(result.recommendedSafetyStock()).isGreaterThan(0.0);
        assertThat(result.reorderPoint()).isGreaterThan(350.0);
        assertThat(result.economicOrderQuantity()).isGreaterThan(500.0);
    }

    @Test
    @DisplayName("ProcurementPlanningService calculates net requirements and allocates to suppliers")
    void testProcurementPlanning() {
        NetRequirementItem netReq = procurementService.calculateNetRequirement(
                batteryPack, plantNode, 1000.0, 300.0, 100.0, LocalDate.now().plusWeeks(2)
        );
        assertThat(netReq.netRequirement()).isEqualTo(600.0);

        SupplierAllocationShare share1 = new SupplierAllocationShare(
                new NodeId("SUPPLIER-A"), batteryPack, 70.0, 420.0, new BigDecimal("100.00"), new BigDecimal("42000.00")
        );
        SupplierAllocationShare share2 = new SupplierAllocationShare(
                new NodeId("SUPPLIER-B"), batteryPack, 30.0, 180.0, new BigDecimal("105.00"), new BigDecimal("18900.00")
        );

        ProcurementPlan plan = procurementService.generateProcurementPlan(
                workspaceId, "Q1 Sourcing Plan", List.of(netReq), List.of(share1, share2)
        );

        assertThat(plan.totalProjectedCost()).isEqualTo(new BigDecimal("60900.00"));
        verify(procurementRepo).save(any());
    }

    @Test
    @DisplayName("ProductionPlanningService generates finite schedule with utilization")
    void testProductionScheduling() {
        ScheduledProductionJob job1 = new ScheduledProductionJob("J1", batteryPack, plantNode, LocalDate.now(), 1, 100.0, 1.0, 5.0, 1);
        ScheduledProductionJob job2 = new ScheduledProductionJob("J2", batteryPack, plantNode, LocalDate.now(), 1, 50.0, 0.5, 2.5, 2);

        FiniteProductionSchedule schedule = productionService.generateFiniteSchedule(
                workspaceId, plantNode, "Shift 1 Schedule", List.of(job1, job2), 10.0
        );

        assertThat(schedule.averageCapacityUtilization()).isEqualTo(0.90);
        verify(productionRepo).save(any());
    }

    @Test
    @DisplayName("SupplyChainNetworkPlanningService evaluates trade-offs across 4 modes")
    void testTransportTradeoffs() {
        List<TransportModeTradeoff> tradeoffs = networkService.evaluateTransportTradeoffs(1000.0, 20.0);
        assertThat(tradeoffs).hasSize(4);
        // Air should be faster but much higher CO2 than Ocean
        TransportModeTradeoff air = tradeoffs.stream().filter(t -> t.mode().name().equals("AIR")).findFirst().orElseThrow();
        TransportModeTradeoff ocean = tradeoffs.stream().filter(t -> t.mode().name().equals("OCEAN")).findFirst().orElseThrow();
        assertThat(air.transitDays()).isLessThan(ocean.transitDays());
        assertThat(air.co2KgPerTon()).isGreaterThan(ocean.co2KgPerTon());
    }

    @Test
    @DisplayName("PlanningScenarioService requires PLAN_APPROVE permission to approve plan")
    void testPlanningScenarioApprovalPermission() {
        PlanningScenarioComparison scenario = new PlanningScenarioComparison(
                PlanningScenarioId.generate(),
                workspaceId,
                "Base Operating Plan",
                PlanningScenarioType.BASE_PLAN,
                PlanningPlanStatus.EVALUATED,
                new BigDecimal("500000.00"),
                98.0,
                3.5,
                45.0,
                "planner1",
                Instant.now()
        );

        when(scenarioRepo.findById(scenario.scenarioId())).thenReturn(Optional.of(scenario));

        UserProfile unauthorizedUser = UserProfile.create("user-viewer", "viewer", "viewer@corp.com", "Viewer", UserRole.VIEWER, java.util.Set.of(workspaceId));
        assertThatThrownBy(() -> scenarioService.approvePlan(scenario.scenarioId(), unauthorizedUser, "Looks good"))
                .isInstanceOf(AccessDeniedException.class);

        UserProfile authorizedUser = UserProfile.create("user-manager", "manager", "manager@corp.com", "Manager", UserRole.SUPPLY_CHAIN_MANAGER, java.util.Set.of(workspaceId));
        PlanningScenarioComparison approved = scenarioService.approvePlan(scenario.scenarioId(), authorizedUser, "Authorized for Q1");

        assertThat(approved.status()).isEqualTo(PlanningPlanStatus.APPROVED);
        verify(auditService).logAction(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
