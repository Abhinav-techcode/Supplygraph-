package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.PostgresDigitalTwinRepositoryAdapter;
import com.supplygraph.adapter.persistence.adapter.TwinGraphStore;
import com.supplygraph.adapter.persistence.repository.SpringDataDigitalTwinStateRepository;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TwinPersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Digital twin durability: graph, BOM and inventory survive a restart")
class TwinPersistenceIntegrationTest {
    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("twin").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired SpringDataDigitalTwinStateRepository states;
    @Autowired TwinGraphStore store;
    @Autowired JdbcTemplate jdbc;

    private DigitalTwinState twin(WorkspaceId ws) {
        Supplier sup = new Supplier(NodeId.of("SUP-1"), "Supplier One", "Taipei, TW", true, 2, Set.of("ISO9001", "IATF16949"), 0.12);
        ManufacturingSite site = new ManufacturingSite(NodeId.of("PLANT-1"), "Plant One", "Stuttgart, DE", true, Set.of("L1", "L2"), "ISO5", 120);
        Warehouse wh = new Warehouse(NodeId.of("WH-1"), "Warehouse", "Frankfurt, DE", true, new BigDecimal("5000"), true, false);
        LogisticsHub hub = new LogisticsHub(NodeId.of("HUB-1"), "Hub", "Rotterdam, NL", true, true, 900, Set.of("RAIL", "ROAD"));
        List<TransportationLane> lanes = List.of(
                new TransportationLane(EdgeId.of("L-1"), NodeId.of("SUP-1"), NodeId.of("HUB-1"), TransportMode.AIR, Duration.ofHours(30), new BigDecimal("4.5"), true),
                new TransportationLane(EdgeId.of("L-2"), NodeId.of("HUB-1"), NodeId.of("PLANT-1"), TransportMode.RAIL, Duration.ofHours(8), new BigDecimal("0.9"), false));
        Component chip = new Component(MaterialId.of("CHIP"), "SKU-CHIP", "Chip", UnitOfMeasure.PIECE, 21);
        FinishedProduct car = new FinishedProduct(MaterialId.of("CAR"), "SKU-CAR", "Car", UnitOfMeasure.PIECE, "EV-LINE", 36);
        BillOfMaterials bom = new BillOfMaterials(List.of(chip, car), List.of(new BomEntry(MaterialId.of("CAR"), MaterialId.of("CHIP"), Quantity.of(4, UnitOfMeasure.PIECE), new BigDecimal("0.02"), 1)));
        DigitalTwinState state = new DigitalTwinState(ws, StateVersion.of(7), new SupplyChainGraph(List.of(sup, site, wh, hub), lanes), bom);
        InventoryAccountId acct = InventoryAccountId.of(NodeId.of("PLANT-1"), MaterialId.of("CHIP"), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId off = InventoryAccountId.of(NodeId.of("PLANT-1"), MaterialId.of("CHIP"), BalanceCategory.EXTERNAL_OFFSET);
        Quantity q = Quantity.of(250, UnitOfMeasure.PIECE);
        state.getInventoryLedger().postTransaction(new InventoryTransaction(TransactionId.generate(), Instant.now(), "SEED", "t",
                List.of(new InventoryPosting("a", acct, PostingType.DEBIT, q), new InventoryPosting("b", off, PostingType.CREDIT, q))));
        return state;
    }

    @Test void graphBomAndInventoryAreRebuiltFromThePersistedTables() {
        WorkspaceId ws = WorkspaceId.of("ws-twin-1");
        jdbc.update("INSERT INTO workspaces (id, name, industry, created_at, updated_at) VALUES (?, ?, 'EV_BATTERY', now(), now()) ON CONFLICT DO NOTHING", ws.value(), ws.value());
        PostgresDigitalTwinRepositoryAdapter writer = new PostgresDigitalTwinRepositoryAdapter(states, store);
        writer.save(twin(ws));

        PostgresDigitalTwinRepositoryAdapter afterRestart = new PostgresDigitalTwinRepositoryAdapter(states, store);   // empty cache = fresh JVM
        DigitalTwinState loaded = afterRestart.findByWorkspaceId(ws).orElseThrow();
        assertThat(loaded.getCurrentVersion()).isEqualTo(StateVersion.of(7));
        assertThat(loaded.getGraph().getNodeCount()).isEqualTo(4);
        assertThat(loaded.getGraph().getLaneCount()).isEqualTo(2);
        Supplier sup = (Supplier) loaded.getGraph().getNode(NodeId.of("SUP-1"));
        assertThat(sup.getTier()).isEqualTo(2);
        assertThat(sup.getCertifications()).containsExactlyInAnyOrder("ISO9001", "IATF16949");
        assertThat(sup.getRiskScore()).isEqualTo(0.12);
        ManufacturingSite site = (ManufacturingSite) loaded.getGraph().getNode(NodeId.of("PLANT-1"));
        assertThat(site.getProductionLines()).containsExactlyInAnyOrder("L1", "L2");
        assertThat(site.getMaxThroughputPerHour()).isEqualTo(120);
        assertThat(((Warehouse) loaded.getGraph().getNode(NodeId.of("WH-1"))).isColdStorageSupported()).isTrue();
        assertThat(((LogisticsHub) loaded.getGraph().getNode(NodeId.of("HUB-1"))).getSupportedFreightModes()).containsExactlyInAnyOrder("RAIL", "ROAD");
        TransportationLane l2 = loaded.getGraph().findLane(EdgeId.of("L-2")).orElseThrow();
        assertThat(l2.isOperational()).isFalse();
        assertThat(l2.getNominalTransitTime()).isEqualTo(Duration.ofHours(8));
        assertThat(loaded.getGraph().getDownstreamReachableNodes(NodeId.of("SUP-1"))).containsExactlyInAnyOrder(NodeId.of("HUB-1"), NodeId.of("PLANT-1"));
        assertThat(((Component) loaded.getBom().getMaterial(MaterialId.of("CHIP"))).getProcurementLeadTimeDays()).isEqualTo(21);
        assertThat(((FinishedProduct) loaded.getBom().getMaterial(MaterialId.of("CAR"))).getProductLine()).isEqualTo("EV-LINE");
        assertThat(loaded.getBom().getDirectComponents(MaterialId.of("CAR"))).hasSize(1);
        assertThat(loaded.getInventoryLedger().getBalance(InventoryAccountId.of(NodeId.of("PLANT-1"), MaterialId.of("CHIP"), BalanceCategory.ON_HAND_AVAILABLE), UnitOfMeasure.PIECE).amount())
                .isEqualByComparingTo("250");
    }

    @Test void savingAgainReplacesTheGraphAndWorkspacesStayIsolated() {
        WorkspaceId a = WorkspaceId.of("ws-twin-a"), b = WorkspaceId.of("ws-twin-b");
        for (WorkspaceId w : List.of(a, b)) jdbc.update("INSERT INTO workspaces (id, name, industry, created_at, updated_at) VALUES (?, ?, 'EV_BATTERY', now(), now()) ON CONFLICT DO NOTHING", w.value(), w.value());
        PostgresDigitalTwinRepositoryAdapter repo = new PostgresDigitalTwinRepositoryAdapter(states, store);
        repo.save(twin(a));
        repo.save(twin(b));   // same node ids in another workspace must not collide
        repo.save(new DigitalTwinState(a, StateVersion.of(8), new SupplyChainGraph(List.of(new Supplier(NodeId.of("ONLY"), "Only", "X", true, 1, Set.of(), 0.0)), List.of()), BillOfMaterials.empty()));
        PostgresDigitalTwinRepositoryAdapter fresh = new PostgresDigitalTwinRepositoryAdapter(states, store);
        assertThat(fresh.findByWorkspaceId(a).orElseThrow().getGraph().getNodeCount()).isEqualTo(1);
        assertThat(fresh.findByWorkspaceId(b).orElseThrow().getGraph().getNodeCount()).isEqualTo(4);
    }
}
