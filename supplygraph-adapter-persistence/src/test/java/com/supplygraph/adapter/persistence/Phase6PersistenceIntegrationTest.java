package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataWorkspaceRepository;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.benchmark.*;
import com.supplygraph.domain.model.provenance.ProvenanceRecord;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase6PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 6 PostgreSQL Persistence Integration Test")
class Phase6PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_phase6_test")
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
    @Autowired private PostgresUserProfileRepositoryAdapter userProfileAdapter;
    @Autowired private PostgresAuditLogRepositoryAdapter auditLogAdapter;
    @Autowired private PostgresProvenanceRepositoryAdapter provenanceAdapter;
    @Autowired private PostgresBenchmarkExperimentRepositoryAdapter benchmarkAdapter;

    @Test
    @DisplayName("Should persist and retrieve all Phase 6 security, audit, provenance, and benchmark entities")
    void shouldPersistAndRetrievePhase6Entities() {
        WorkspaceId wsId = WorkspaceId.of("WS-PHASE6-01");
        workspaceRepo.save(new WorkspaceJpaEntity(wsId.value(), "Phase 6 Enterprise Workspace", "SEMICONDUCTOR", Instant.now(), Instant.now()));

        // 1. User Profile & RBAC
        UserProfile user = UserProfile.create(
                "USR-ENG-01",
                "lead_architect",
                "architect@supplygraph.io",
                "Chief Supply Chain Architect",
                UserRole.SUPPLY_CHAIN_MANAGER,
                Set.of(wsId)
        );
        userProfileAdapter.save(user);

        Optional<UserProfile> loadedUser = userProfileAdapter.findById(user.id());
        assertThat(loadedUser).isPresent();
        assertThat(loadedUser.get().username()).isEqualTo("lead_architect");
        assertThat(loadedUser.get().role()).isEqualTo(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThat(loadedUser.get().hasAccessToWorkspace(wsId)).isTrue();

        Optional<UserProfile> loadedByUsername = userProfileAdapter.findByUsername("lead_architect");
        assertThat(loadedByUsername).isPresent();

        // 2. Immutable Audit Log with SHA-256 Signature
        AuditLogEntry audit = AuditLogEntry.record(
                user.id(),
                wsId,
                AuditAction.PLAN_APPROVED.name(),
                "PLAN-RECOVER-404",
                "v1",
                "v2",
                "Authorized emergency wafer allocation",
                "CONF-SEMICONDUCTOR-01"
        );
        auditLogAdapter.save(audit);

        Optional<AuditLogEntry> loadedAudit = auditLogAdapter.findById(audit.id());
        assertThat(loadedAudit).isPresent();
        assertThat(loadedAudit.get().action()).isEqualTo("PLAN_APPROVED");
        assertThat(loadedAudit.get().isSignatureValid()).isTrue();

        List<AuditLogEntry> auditTrail = auditLogAdapter.findByWorkspaceId(wsId);
        assertThat(auditTrail).hasSize(1);

        // 3. Data Provenance
        ProvenanceRecord provenance = ProvenanceRecord.of(
                wsId,
                "WAFER-300MM-SILICON",
                "MATERIAL",
                ProvenanceType.REAL_IMPORTED,
                "TSMC-ERP-B2B-GATEWAY",
                "v2",
                Map.of("purityGrade", "99.9999%", "fabLocation", "HSINCHU-FAB-12")
        );
        provenanceAdapter.save(provenance);

        Optional<ProvenanceRecord> loadedProvenance = provenanceAdapter.findById(provenance.id());
        assertThat(loadedProvenance).isPresent();
        assertThat(loadedProvenance.get().type()).isEqualTo(ProvenanceType.REAL_IMPORTED);
        assertThat(loadedProvenance.get().attributes()).containsEntry("purityGrade", "99.9999%");

        // 4. Research Benchmark Experiment
        BenchmarkMetrics metrics = new BenchmarkMetrics(
                0.98, 1.0, 0.02, 0.0,
                5, 12, 1, 45L, 3L, 22000.0, 99.2, 1.0, 15L, 85L, 12L
        );
        BenchmarkExperimentRecord experiment = BenchmarkExperimentRecord.create(
                "TSMC Cleanroom Schedule Contention",
                BenchmarkScenarioType.MACHINE_SCHEDULING_OVERLAP,
                BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING,
                "FIXTURE-SEMI-01",
                "v2",
                12345L,
                5,
                metrics,
                "Validated with 5 trials"
        );
        benchmarkAdapter.save(experiment);

        Optional<BenchmarkExperimentRecord> loadedExperiment = benchmarkAdapter.findById(experiment.id());
        assertThat(loadedExperiment).isPresent();
        assertThat(loadedExperiment.get().scenario()).isEqualTo(BenchmarkScenarioType.MACHINE_SCHEDULING_OVERLAP);
        assertThat(loadedExperiment.get().metrics().conflictPrecision()).isEqualTo(0.98);
        assertThat(loadedExperiment.get().metrics().validActionsPreserved()).isEqualTo(12);

        List<BenchmarkExperimentRecord> byScenario = benchmarkAdapter.findByScenario(BenchmarkScenarioType.MACHINE_SCHEDULING_OVERLAP);
        assertThat(byScenario).hasSize(1);
    }
}

