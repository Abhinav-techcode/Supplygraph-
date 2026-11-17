package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataWorkspaceRepository;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.*;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.monitoring.*;
import com.supplygraph.domain.model.reassessment.ImpactReassessment;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningStatus;
import com.supplygraph.domain.model.twin.StateVersion;
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

@SpringBootTest(classes = Phase5PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 5 PostgreSQL Persistence Integration Test")
class Phase5PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_test")
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

    @Autowired private SpringDataWorkspaceRepository workspaceRepo;
    @Autowired private PostgresOperationalEventRepositoryAdapter eventAdapter;
    @Autowired private PostgresMonitoringRuleRepositoryAdapter ruleAdapter;
    @Autowired private PostgresRiskAlertRepositoryAdapter alertAdapter;
    @Autowired private PostgresImpactReassessmentRepositoryAdapter reassessmentAdapter;
    @Autowired private PostgresReplanningRunRepositoryAdapter replanningAdapter;

    @Test
    @DisplayName("Should persist and retrieve all Phase 5 entities against PostgreSQL with Flyway")
    void shouldPersistAndRetrievePhase5Entities() {
        WorkspaceId wsId = WorkspaceId.of("WS-PHASE5-01");
        workspaceRepo.save(new WorkspaceJpaEntity(wsId.value(), "Phase 5 Workspace", "AUTOMOTIVE", Instant.now(), Instant.now()));

        // 1. Operational Event
        OperationalEvent event = OperationalEvent.createIngested(
                EventType.SUPPLIER_DELIVERY_DELAYED,
                EventSource.EXTERNAL_INTEGRATION,
                Instant.now(),
                "SUP-101",
                "NODE",
                wsId,
                Map.of("delayHours", "24"),
                EventProvenance.imported("ERP", "BATCH-99", "admin"),
                "IDEMP-P5-1"
        );
        event.markValidated();
        event.markProcessed();
        eventAdapter.save(event);

        Optional<OperationalEvent> loadedEvent = eventAdapter.findById(event.getEventId());
        assertThat(loadedEvent).isPresent();
        assertThat(loadedEvent.get().getEventType()).isEqualTo(EventType.SUPPLIER_DELIVERY_DELAYED);
        assertThat(loadedEvent.get().getStatus()).isEqualTo(EventStatus.PROCESSED);

        // 2. Monitoring Rule
        MonitoringRule rule = MonitoringRule.of(
                "RULE-P5-1", "Lead Time Surge Alert", "Alerts when lead time surges",
                "AUTOMOTIVE", MonitoringSeverity.HIGH, BigDecimal.valueOf(24),
                MonitoringEvaluationTrigger.ON_EVENT, "SUPPLIER_DELAY_THRESHOLD"
        );
        ruleAdapter.save(rule);

        Optional<MonitoringRule> loadedRule = ruleAdapter.findById(rule.getId());
        assertThat(loadedRule).isPresent();
        assertThat(loadedRule.get().getName()).isEqualTo("Lead Time Surge Alert");

        // 3. Risk Alert
        RiskAlert alert = RiskAlert.createActive(
                wsId, rule.getId(), MonitoringSeverity.HIGH, "Delivery Delay Alert",
                "Delivery delayed by 24h", "SUP-101", "NODE", event.getEventId(),
                StateVersion.of(1L), Map.of("delayHours", "24")
        );
        alertAdapter.save(alert);

        Optional<RiskAlert> loadedAlert = alertAdapter.findById(alert.getId());
        assertThat(loadedAlert).isPresent();
        assertThat(loadedAlert.get().getStatus()).isEqualTo(RiskAlertStatus.ACTIVE);

        // 4. Impact Reassessment
        ImpactReassessment reassessment = ImpactReassessment.create(
                wsId, event.getEventId(), StateVersion.of(1L), StateVersion.of(2L),
                Set.of(NodeId.of("SUP-101")), Set.of(EdgeId.of("LANE-1")), Set.of(MaterialId.of("MAT-1")),
                List.of("Preserved: Downstream lines"), List.of("Invalidated: Buffer"),
                List.of("ASSESS-1"), List.of("PLAN-1")
        );
        reassessmentAdapter.save(reassessment);

        Optional<ImpactReassessment> loadedReassessment = reassessmentAdapter.findById(reassessment.getId());
        assertThat(loadedReassessment).isPresent();
        assertThat(loadedReassessment.get().getAffectedNodeIds()).contains(NodeId.of("SUP-101"));

        // 5. Replanning Run
        ReplanningRun run = ReplanningRun.start(wsId, alert.getId(), null, "Adaptive replanning test");
        run.advanceStatus(ReplanningStatus.ASSESSING, "Scanning scope");
        run.advanceStatus(ReplanningStatus.REPLANNING, "Invoking agents");
        run.addAffectedAgents(List.of("ProcurementAgent"));
        run.advanceStatus(ReplanningStatus.READY_FOR_REVIEW, "Replanned successfully");
        replanningAdapter.save(run);

        Optional<ReplanningRun> loadedRun = replanningAdapter.findById(run.getId());
        assertThat(loadedRun).isPresent();
        assertThat(loadedRun.get().getStatus()).isEqualTo(ReplanningStatus.READY_FOR_REVIEW);
        assertThat(loadedRun.get().getAffectedAgentIds()).contains("ProcurementAgent");
    }
}
