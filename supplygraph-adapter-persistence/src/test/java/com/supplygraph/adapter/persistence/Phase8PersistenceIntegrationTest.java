package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.domain.model.explainability.*;
import com.supplygraph.domain.model.pareto.*;
import com.supplygraph.domain.model.provenance.graph.*;
import com.supplygraph.domain.model.resilience.advanced.*;
import com.supplygraph.domain.model.sensitivity.*;
import com.supplygraph.domain.model.whatif.*;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase8PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 8 PostgreSQL Persistence Integration Test")
class Phase8PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_phase8_test")
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

    @Autowired
    private PostgresExplainableRecommendationPersistenceAdapter recommendationAdapter;

    @Autowired
    private PostgresDecisionProvenancePersistenceAdapter provenanceAdapter;

    @Autowired
    private PostgresWhatIfExperimentPersistenceAdapter whatIfAdapter;

    @Autowired
    private PostgresParetoEvaluationPersistenceAdapter paretoAdapter;

    @Autowired
    private PostgresSensitivityAnalysisPersistenceAdapter sensitivityAdapter;

    @Autowired
    private PostgresAdvancedResiliencePersistenceAdapter resilienceAdapter;

    @Test
    @DisplayName("Should persist and retrieve explainable recommendation with human approval lifecycle")
    void testExplainableRecommendationPersistence() {
        RecommendationId recId = RecommendationId.generate();
        ExplainableRecommendation rec = ExplainableRecommendation.createPending(
                recId,
                "DISR-ROTTERDAM-01",
                "V1.0",
                "Expedited Reroute via Air Freight",
                "High confidence reroute mitigating 14-day harbor congestion",
                List.of("Reserve air cargo space", "Reallocate regional buffers"),
                BigDecimal.valueOf(74000.00),
                36.0,
                98.5,
                List.of(DecisionEvidence.of("E1", "AIS Telemetry", "Harbor Radar", 0.95, Instant.now(), "14-day queue confirmed")),
                List.of(DecisionConstraint.of("C1", "Supplier weekly cap", DecisionConstraint.ConstraintType.HARD, true, "Satisfied")),
                List.of(DecisionTradeOff.of("Cost vs Speed", "$74k", "36 hrs", "Premium freight minimizes stockout risk", 0.8)),
                Map.of("VaR 95%", "Bounded between -5% and +10%")
        );

        recommendationAdapter.save(rec);

        Optional<ExplainableRecommendation> found = recommendationAdapter.findById(recId);
        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("Expedited Reroute via Air Freight");
        assertThat(found.get().requiresHumanApproval()).isTrue();
        assertThat(found.get().approved()).isFalse();

        // Simulate human approval
        ExplainableRecommendation approved = found.get().withApproval("USR-SCM-01", Instant.now());
        recommendationAdapter.save(approved);

        Optional<ExplainableRecommendation> updated = recommendationAdapter.findById(recId);
        assertThat(updated).isPresent();
        assertThat(updated.get().approved()).isTrue();
        assertThat(updated.get().approvedBy()).isEqualTo("USR-SCM-01");
    }

    @Test
    @DisplayName("Should persist and retrieve 13-stage decision provenance graph")
    void testDecisionProvenanceGraphPersistence() {
        DecisionProvenanceNode n1 = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-T1"),
                DecisionProvenanceNodeType.DISRUPTION,
                "Port Bottleneck",
                "DISR-01",
                Instant.now(),
                Map.of("severity", "CRITICAL")
        );
        DecisionProvenanceNode n2 = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-T2"),
                DecisionProvenanceNodeType.HUMAN_DECISION,
                "Human Approval",
                "APP-01",
                Instant.now(),
                Map.of("approver", "USR-01")
        );

        DecisionProvenanceEdge e1 = DecisionProvenanceEdge.of(
                "DPE-T1",
                n1.id(),
                n2.id(),
                DecisionProvenanceEdgeType.APPROVES,
                "Approved after analysis",
                Instant.now()
        );

        DecisionProvenanceGraph graph = DecisionProvenanceGraph.create(
                "WS-TEST",
                "DISR-01",
                List.of(n1, n2),
                List.of(e1)
        );

        provenanceAdapter.saveGraph(graph);

        Optional<DecisionProvenanceGraph> found = provenanceAdapter.findByRootDisruptionId("DISR-01");
        assertThat(found).isPresent();
        assertThat(found.get().getAllNodes()).hasSize(2);
        assertThat(found.get().edges()).hasSize(1);
    }

    @Test
    @DisplayName("Should persist and retrieve What-If comparison results")
    void testWhatIfPersistence() {
        WhatIfScenarioId scId = WhatIfScenarioId.generate();
        WhatIfComparisonResult result = WhatIfComparisonResult.of(
                scId,
                "Air Freight Stress",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(68000),
                12.0,
                5.0,
                92.0,
                97.0,
                65.0,
                78.0,
                36.0,
                -7.0,
                "Air freight scenario reduces recovery time by 7 days at 36% cost premium",
                Instant.now()
        );

        whatIfAdapter.save(result);

        Optional<WhatIfComparisonResult> found = whatIfAdapter.findByScenarioId(scId);
        assertThat(found).isPresent();
        assertThat(found.get().scenarioName()).isEqualTo("Air Freight Stress");
        assertThat(found.get().costVariancePct()).isEqualTo(36.0);
    }

    @Test
    @DisplayName("Should persist and retrieve multi-objective Pareto evaluations")
    void testParetoPersistence() {
        String evalId = "PAR-TEST-01";
        ParetoSolution s1 = ParetoSolution.of(
                ParetoSolutionId.generate(),
                "ALT-1",
                "Option 1",
                BigDecimal.valueOf(50000),
                24.0,
                100.0,
                2.0,
                0.2,
                true,
                0.95,
                "Optimal"
        );

        ParetoFrontier frontier = new ParetoFrontier(evalId, "DISR-P1", List.of(s1), List.of(s1), Instant.now());
        paretoAdapter.save(frontier);

        Optional<ParetoFrontier> found = paretoAdapter.findByEvaluationId(evalId);
        assertThat(found).isPresent();
        assertThat(found.get().frontierSolutions()).hasSize(1);
    }

    @Test
    @DisplayName("Should persist and retrieve sensitivity analysis reports")
    void testSensitivityPersistence() {
        String rptId = "SEN-TEST-01";
        SensitivityTrialResult t1 = SensitivityTrialResult.of(
                SensitivityTrialId.generate(),
                SensitivityParameter.LEAD_TIME,
                10.0,
                BigDecimal.valueOf(54000),
                11.0,
                94.0,
                0.8
        );

        SensitivityAnalysisReport report = SensitivityAnalysisReport.of(
                rptId,
                "WS-SEN",
                "ALT-01",
                BigDecimal.valueOf(50000),
                10.0,
                95.0,
                List.of(t1),
                0.85,
                Instant.now()
        );

        sensitivityAdapter.save(report);

        Optional<SensitivityAnalysisReport> found = sensitivityAdapter.findById(rptId);
        assertThat(found).isPresent();
        assertThat(found.get().overallRobustnessScore()).isEqualTo(0.85);
        assertThat(found.get().trials()).hasSize(1);
    }

    @Test
    @DisplayName("Should persist and retrieve advanced resilience analytics")
    void testAdvancedResiliencePersistence() {
        String assessId = "RES-TEST-01";
        NodeResilienceMetric n1 = NodeResilienceMetric.of("SUP-01", "Supplier 1", 14.0, 35.0, 2, 0.7);

        AdvancedResilienceMetrics metrics = AdvancedResilienceMetrics.of(
                assessId,
                "WS-ADV-RES",
                16.5,
                28.0,
                0.25,
                0.75,
                2,
                72.5,
                List.of(n1),
                Instant.now()
        );

        resilienceAdapter.save(metrics);

        Optional<AdvancedResilienceMetrics> found = resilienceAdapter.findLatestByWorkspaceId("WS-ADV-RES");
        assertThat(found).isPresent();
        assertThat(found.get().timeToSurviveDays()).isEqualTo(16.5);
        assertThat(found.get().nodeResilienceBreakdown()).hasSize(1);
    }
}
