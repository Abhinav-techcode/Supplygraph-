package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.domain.model.integration.*;
import com.supplygraph.domain.model.intelligence.*;
import com.supplygraph.domain.model.quality.*;
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

@SpringBootTest(classes = Phase7PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 7 PostgreSQL Persistence Integration Test")
class Phase7PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_phase7_test")
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
    private PostgresImportJobPersistenceAdapter importJobAdapter;

    @Autowired
    private PostgresDataQualityPersistenceAdapter qualityAdapter;

    @Autowired
    private PostgresSupplierIntelligencePersistenceAdapter supplierAdapter;

    @Autowired
    private PostgresMaterialIntelligencePersistenceAdapter materialAdapter;

    @Autowired
    private PostgresResiliencePersistenceAdapter resilienceAdapter;

    @Test
    @DisplayName("Should persist and retrieve enterprise data import job with failures")
    void shouldPersistAndRetrieveImportJob() {
        String ws = "WS-PH7-01";
        ImportJob job = ImportJob.createNew(ws, ImportSourceType.CSV, DataCategory.SUPPLIER, "suppliers_2026.csv", "sha256_mock_hash", "USR-ADMIN");
        job.markValidated(100, 95, 5, List.of(
                ImportRecordFailure.of(12, "lead_time", "-5", "NEGATIVE_LEAD_TIME", "Lead time cannot be negative", "Set lead time >= 0")
        ));
        importJobAdapter.save(job);

        Optional<ImportJob> loaded = importJobAdapter.findById(job.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().workspaceId()).isEqualTo(ws);
        assertThat(loaded.get().category()).isEqualTo(DataCategory.SUPPLIER);
        assertThat(loaded.get().status()).isEqualTo(ImportStatus.VALIDATED);
        assertThat(loaded.get().failures()).hasSize(1);
        assertThat(loaded.get().failures().get(0).field()).isEqualTo("lead_time");
    }

    @Test
    @DisplayName("Should persist and retrieve data quality report and findings")
    void shouldPersistAndRetrieveDataQualityReport() {
        String ws = "WS-PH7-02";
        DataQualityFinding finding = DataQualityFinding.of(
                "DQR-CONS-01",
                QualityDimension.CONSISTENCY,
                "ACC-WH-01",
                "reservedQuantity",
                "1500 > 1000",
                "Reserved inventory exceeds physical on-hand quantity",
                DataQualitySeverity.CRITICAL,
                "Decommit expired reservations"
        );
        DataQualityReport report = DataQualityReport.of(
                ws,
                82.5,
                Map.of(QualityDimension.CONSISTENCY, 75.0, QualityDimension.COMPLETENESS, 90.0),
                List.of(finding),
                50,
                48,
                2
        );
        qualityAdapter.save(report);

        Optional<DataQualityReport> loaded = qualityAdapter.findLatestByWorkspaceId(ws);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().overallScore()).isEqualTo(82.5);
        assertThat(loaded.get().findings()).hasSize(1);
        assertThat(loaded.get().findings().get(0).dimension()).isEqualTo(QualityDimension.CONSISTENCY);
    }

    @Test
    @DisplayName("Should persist and retrieve supplier risk profiles and metrics")
    void shouldPersistAndRetrieveSupplierProfiles() {
        String ws = "WS-PH7-03";
        SupplierRiskProfile profile = new SupplierRiskProfile(
                "SUP-MUNICH-01",
                "Munich Microelectronics Fab",
                1,
                "DEU",
                0.28,
                List.of(),
                false,
                4,
                2,
                "QUALIFIED"
        );
        supplierAdapter.saveAll(ws, List.of(profile));

        List<SupplierRiskProfile> loaded = supplierAdapter.findByWorkspaceId(ws);
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).supplierId()).isEqualTo("SUP-MUNICH-01");
        assertThat(loaded.get(0).overallRiskScore()).isEqualTo(0.28);
    }

    @Test
    @DisplayName("Should persist and retrieve material intelligence profiles with runway")
    void shouldPersistAndRetrieveMaterialProfiles() {
        String ws = "WS-PH7-04";
        MaterialIntelligenceProfile profile = new MaterialIntelligenceProfile(
                "MAT-MCU-32",
                "Automotive 32-bit MCU",
                "SEMICONDUCTOR",
                MaterialCriticalityLevel.VERY_HIGH,
                "SUP-MUNICH-01",
                90,
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(1800),
                BigDecimal.valueOf(700),
                BigDecimal.ZERO,
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(36.0),
                0,
                3,
                "AUTOMOTIVE"
        );
        materialAdapter.saveAll(ws, List.of(profile));

        Optional<MaterialIntelligenceProfile> loaded = materialAdapter.findByMaterialId(ws, "MAT-MCU-32");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().criticality()).isEqualTo(MaterialCriticalityLevel.VERY_HIGH);
        assertThat(loaded.get().inventoryRunwayDays()).isEqualByComparingTo("36.0");
        assertThat(loaded.get().isRunwayBelowLeadTime()).isTrue();
    }

    @Test
    @DisplayName("Should persist and retrieve network resilience assessment")
    void shouldPersistAndRetrieveResilienceAssessment() {
        String ws = "WS-PH7-05";
        NetworkResilienceAssessment assessment = NetworkResilienceAssessment.of(
                ws,
                78.4,
                2,
                2500.0,
                4,
                0.65,
                1,
                List.of(
                        ResilienceFactor.of("Single Point of Failure Exposure", 60.0, 0.25, "2 SPOFs identified"),
                        ResilienceFactor.of("Supplier Market Concentration (HHI)", 75.0, 0.20, "Moderate concentration")
                ),
                List.of("Supplier SPOF on MCU-32"),
                List.of("Qualify second-source fab")
        );
        resilienceAdapter.save(assessment);

        Optional<NetworkResilienceAssessment> loaded = resilienceAdapter.findLatestByWorkspaceId(ws);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().overallResilienceIndex()).isEqualTo(78.4);
        assertThat(loaded.get().singlePointsOfFailureCount()).isEqualTo(2);
    }
}
