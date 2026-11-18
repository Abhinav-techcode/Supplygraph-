package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.PostgresCarbonEmissionPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresCircularLedgerPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresEnvironmentalScenarioPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresSupplierEsgPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresTraceabilityPersistenceAdapter;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecordId;
import com.supplygraph.domain.model.sustainability.CircularFlowType;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenario;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenarioId;
import com.supplygraph.domain.model.sustainability.EsgScorecardId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.GenealogyEdge;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeId;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase12PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 12 Sustainability & Traceability PostgreSQL Persistence Integration Test")
class Phase12PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_p12_test")
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

    @Autowired private PostgresCarbonEmissionPersistenceAdapter carbonAdapter;
    @Autowired private PostgresTraceabilityPersistenceAdapter traceAdapter;
    @Autowired private PostgresSupplierEsgPersistenceAdapter esgAdapter;
    @Autowired private PostgresCircularLedgerPersistenceAdapter circularAdapter;
    @Autowired private PostgresEnvironmentalScenarioPersistenceAdapter envAdapter;

    private final WorkspaceId workspaceId = new WorkspaceId("ws-p12-test");
    private final NodeId plantBerlin = new NodeId("PLANT-BERLIN");
    private final MaterialId batteryPack = new MaterialId("MAT-BP-4680");

    @Test
    @DisplayName("Carbon emission record persists and retrieves by workspace")
    void testCarbonRecordPersistence() {
        CarbonEmissionRecord record = new CarbonEmissionRecord(
                CarbonEmissionRecordId.generate(),
                workspaceId,
                plantBerlin,
                EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY,
                "Plant Grid Power",
                50000.0,
                "kWh",
                0.385,
                19250.0,
                Instant.now()
        );

        carbonAdapter.save(record);

        Optional<CarbonEmissionRecord> loaded = carbonAdapter.findById(record.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().scope()).isEqualTo(EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY);
        assertThat(loaded.get().getTotalEmissionsTons()).isEqualTo(19.25);
    }

    @Test
    @DisplayName("GenealogyDAG persists nodes and edges, restoring topological traceability")
    void testGenealogyDagPersistence() {
        GenealogyDAG dag = new GenealogyDAG();

        TraceabilityNode mine = new TraceabilityNode(
                new TraceabilityNodeId("TRC-MINE-01"), "MINE-BATCH-01", TraceabilityNodeType.RAW_MATERIAL_ORIGIN,
                new MaterialId("MAT-COBALT-ORE"), new NodeId("MINE-DRC"), 5000.0, "kg", "Congo Mining Co", Instant.now()
        );
        TraceabilityNode refined = new TraceabilityNode(
                new TraceabilityNodeId("TRC-REF-01"), "REF-BATCH-01", TraceabilityNodeType.SUPPLIER_BATCH,
                new MaterialId("MAT-COBALT-SULFATE"), new NodeId("SUPPLIER-FINLAND"), 4800.0, "kg", "Kokkola Refinery", Instant.now()
        );

        dag.addNode(mine);
        dag.addNode(refined);
        dag.addEdge(new GenealogyEdge(mine.id(), refined.id(), 4800.0, "Hydrometallurgical Refining"));

        traceAdapter.saveDag(workspaceId, dag);

        Optional<GenealogyDAG> loaded = traceAdapter.findDagByWorkspaceId(workspaceId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAllNodes()).hasSize(2);
        assertThat(loaded.get().getAllEdges()).hasSize(1);
        assertThat(loaded.get().backwardTrace(refined.id())).extracting(TraceabilityNode::id).contains(mine.id());
    }

    @Test
    @DisplayName("Supplier ESG scorecard persists and validates CBAM & EUDR compliance")
    void testSupplierEsgPersistence() {
        SupplierEsgScorecard card = new SupplierEsgScorecard(
                EsgScorecardId.generate(),
                workspaceId,
                new NodeId("SUPPLIER-NORDIC"),
                88.0,
                92.0,
                85.0,
                88.3,
                true,
                true,
                true,
                Instant.now()
        );

        esgAdapter.save(card);

        Optional<SupplierEsgScorecard> loaded = esgAdapter.findById(card.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().isFullyCompliant()).isTrue();
        assertThat(loaded.get().compositeEsgScore()).isEqualTo(88.3);
    }

    @Test
    @DisplayName("Circular ledger entry persists material recovery and avoided virgin material")
    void testCircularLedgerPersistence() {
        CircularLedgerEntry entry = new CircularLedgerEntry(
                com.supplygraph.domain.model.sustainability.CircularLedgerEntryId.generate(),
                workspaceId,
                batteryPack,
                CircularFlowType.BATTERY_CELL_RECYCLING,
                2000.0,
                94.0,
                1880.0,
                1786.0,
                Instant.now()
        );

        circularAdapter.save(entry);

        Optional<CircularLedgerEntry> loaded = circularAdapter.findById(entry.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().recoveredQuantity()).isEqualTo(1880.0);
        assertThat(loaded.get().virginMaterialAvoidedKg()).isEqualTo(1786.0);
    }

    @Test
    @DisplayName("Environmental scenario persists projected emissions and abatement cost per ton")
    void testEnvironmentalScenarioPersistence() {
        EnvironmentalScenario scenario = new EnvironmentalScenario(
                EnvironmentalScenarioId.generate(),
                workspaceId,
                "Electrified Trucking Fleet",
                5000.0,
                3200.0,
                36.0,
                new BigDecimal("90000.00"),
                new BigDecimal("50.00"),
                Instant.now()
        );

        envAdapter.save(scenario);

        Optional<EnvironmentalScenario> loaded = envAdapter.findById(scenario.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().reductionPercentage()).isEqualTo(36.0);
        assertThat(loaded.get().costPerTonCo2AvoidedUsd()).isEqualByComparingTo("50.00");
    }
}

