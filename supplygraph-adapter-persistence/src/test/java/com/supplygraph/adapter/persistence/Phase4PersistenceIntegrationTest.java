package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import com.supplygraph.adapter.persistence.repository.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.NodeType;
import com.supplygraph.domain.model.impact.*;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.engine.SimulationResult;
import com.supplygraph.simulation.montecarlo.MonteCarloSummaryResult;
import com.supplygraph.simulation.optimization.OptimizationDecision;
import com.supplygraph.simulation.optimization.OptimizationResult;
import com.supplygraph.simulation.optimization.SolverStatus;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase4PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 4 PostgreSQL Persistence Integration Test")
class Phase4PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_phase4_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private SpringDataWorkspaceRepository workspaceRepo;

    @Autowired
    private SpringDataDisruptionRepository disruptionRepo;

    @Autowired
    private SpringDataImpactAssessmentRepository assessmentRepo;

    @Autowired
    private SpringDataSimulationRunRepository simulationRepo;

    @Autowired
    private SpringDataMonteCarloRunRepository monteCarloRepo;

    @Autowired
    private SpringDataOptimizationRunRepository optimizationRepo;

    @Autowired
    private SpringDataRecoveryPlanRepository recoveryPlanRepo;

    @Test
    @DisplayName("Verify complete persistence lifecycle for Phase 4 entities against live PostgreSQL")
    void testPhase4EntitiesPersistence() {
        // 1. Workspace
        String wsId = "ws-phase4-test";
        workspaceRepo.save(new WorkspaceJpaEntity(wsId, "Phase 4 Test Workspace", "AUTOMOTIVE", Instant.now(), Instant.now()));

        // 2. Disruption
        Disruption disruption = Disruption.createDraft(
                WorkspaceId.of(wsId), "AUTOMOTIVE", "CHIP_SHORTAGE", "Silicon Fab Flood", "Flooding in cleanroom",
                DisruptionCategory.MANUFACTURING_SITE_OUTAGE, DisruptionSeverity.CRITICAL,
                Set.of(NodeId.of("FAB-1")), Set.of(EdgeId.of("LANE-1")), Set.of(MaterialId.of("MCU-32")),
                Instant.now(), Instant.now().plusSeconds(86400 * 14),
                new DisruptionImpactParameters(BigDecimal.valueOf(100), 86400L * 14, 14, BigDecimal.valueOf(50000), "PIECES", Map.of()),
                "Official meteorological force majeure report"
        );

        PostgresDisruptionRepositoryAdapter disruptionAdapter =
                new PostgresDisruptionRepositoryAdapter(disruptionRepo);
        disruptionAdapter.save(disruption);

        Optional<Disruption> loadedDisruption = disruptionAdapter.findById(disruption.id());
        assertThat(loadedDisruption).isPresent();
        assertThat(loadedDisruption.get().title()).isEqualTo("Silicon Fab Flood");
        assertThat(loadedDisruption.get().severity()).isEqualTo(DisruptionSeverity.CRITICAL);

        // 3. Impact Assessment
        ImpactAssessment assessment = ImpactAssessment.create(
                disruption.id(),
                WorkspaceId.of(wsId),
                StateVersion.of(1L),
                ImpactSeverity.CRITICAL,
                List.of(new NodeImpact(NodeId.of("FAB-1"), "Fab 1", NodeType.MANUFACTURING_SITE, ImpactType.DIRECT, "Flooded", BigDecimal.valueOf(100))),
                List.of(new MaterialShortageImpact(MaterialId.of("MCU-32"), "MCU-32", BigDecimal.valueOf(50000), BigDecimal.ZERO, BigDecimal.valueOf(50000), "PIECES", Instant.now())),
                List.of(),
                List.of(),
                BigDecimal.valueOf(2500000),
                List.of("Severe shortage risk"),
                List.of("Fixed BOM consumption"),
                List.of()
        );

        PostgresImpactAssessmentRepositoryAdapter assessmentAdapter =
                new PostgresImpactAssessmentRepositoryAdapter(assessmentRepo);
        assessmentAdapter.save(assessment);

        Optional<ImpactAssessment> loadedAssessment = assessmentAdapter.findById(assessment.id());
        assertThat(loadedAssessment).isPresent();
        assertThat(loadedAssessment.get().overallSeverity()).isEqualTo(ImpactSeverity.CRITICAL);
        assertThat(loadedAssessment.get().totalEstimatedCost()).isEqualByComparingTo(BigDecimal.valueOf(2500000));

        // 4. Simulation Run
        SimulationResult simResult = new SimulationResult(
                "SIM-TEST-1", "SCENARIO-AUTO", StateVersion.of(1L), Instant.now(), Instant.now().plusSeconds(3600),
                45, List.of(), List.of(), List.of(), List.of(), 92.5, BigDecimal.valueOf(1000), 86400L * 3,
                "COMPLETED", List.of(), List.of()
        );
        PostgresSimulationRunRepositoryAdapter simAdapter =
                new PostgresSimulationRunRepositoryAdapter(simulationRepo);
        simAdapter.save(simResult, WorkspaceId.of(wsId), disruption.id(), "PLAN-1");

        assertThat(simAdapter.findById("SIM-TEST-1")).isPresent();

        // 5. Monte Carlo Run
        MonteCarloSummaryResult mcResult = new MonteCarloSummaryResult(
                "MC-TEST-1", 12345L, 50, 4.2, 4.0, 5.5, 6.0,
                BigDecimal.valueOf(1200), BigDecimal.valueOf(2500), 0.12, 0.88, BigDecimal.valueOf(150000),
                "COMPLETED", List.of()
        );
        PostgresMonteCarloRunRepositoryAdapter mcAdapter =
                new PostgresMonteCarloRunRepositoryAdapter(monteCarloRepo);
        mcAdapter.save(mcResult, WorkspaceId.of(wsId), disruption.id(), "PLAN-1");

        assertThat(mcAdapter.findById("MC-TEST-1")).isPresent();

        // 6. Optimization Run
        OptimizationResult optResult = new OptimizationResult(
                "OPT-TEST-1", SolverStatus.OPTIMAL, 45000.0,
                List.of(new OptimizationDecision("DEC-1", "ALLOCATE", "Allocate 1000 units", "SUP-2", null, BigDecimal.valueOf(1000), 86400L * 2, BigDecimal.valueOf(45000), 2.0)),
                BigDecimal.ZERO, 2.0, BigDecimal.valueOf(45000), 35L, List.of("Solved optimal"), List.of("Feasible")
        );
        PostgresOptimizationRunRepositoryAdapter optAdapter =
                new PostgresOptimizationRunRepositoryAdapter(optimizationRepo);
        optAdapter.save(optResult, WorkspaceId.of(wsId), disruption.id());

        assertThat(optAdapter.findById("OPT-TEST-1")).isPresent();

        // 7. Recovery Plan & Human Approval
        RecoveryPlan plan = RecoveryPlan.createDraft(
                disruption.id(), WorkspaceId.of(wsId), StateVersion.of(1L), assessment.id(),
                "Plan Alpha: Strategic Secondary Sourcing", "Reroutes supply to qualified secondary foundry",
                List.of(new PlanAction("ACT-1", PlanActionType.ALTERNATIVE_SUPPLIER, "Procure from secondary foundry", NodeId.of("FAB-2"), MaterialId.of("MCU-32"), BigDecimal.valueOf(50000), "PIECES", BigDecimal.valueOf(45000), 48L, Map.of())),
                PlanComparisonMetrics.defaultMetrics()
        );
        RecoveryPlan validated = plan.markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now());
        RecoveryPlan approved = validated.approve("VP_AUTOMOTIVE_OPERATIONS", "Approved least risk path", StateVersion.of(1L), Instant.now());

        PostgresRecoveryPlanRepositoryAdapter planAdapter =
                new PostgresRecoveryPlanRepositoryAdapter(recoveryPlanRepo);
        planAdapter.save(approved);

        Optional<RecoveryPlan> loadedPlan = planAdapter.findById(plan.id());
        assertThat(loadedPlan).isPresent();
        assertThat(loadedPlan.get().status()).isEqualTo(RecoveryPlanStatus.APPROVED);
        assertThat(loadedPlan.get().approval()).isNotNull();
        assertThat(loadedPlan.get().approval().approvedBy()).isEqualTo("VP_AUTOMOTIVE_OPERATIONS");
    }
}
