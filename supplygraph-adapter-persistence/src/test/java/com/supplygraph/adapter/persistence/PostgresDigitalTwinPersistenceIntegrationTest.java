package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.PostgresDigitalTwinRepositoryAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresInventoryLedgerRepositoryAdapter;
import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataWorkspaceRepository;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PostgresDigitalTwinPersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("PostgreSQL 16 Digital Twin & Inventory Persistence Integration Test")
class PostgresDigitalTwinPersistenceIntegrationTest {

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private SpringDataWorkspaceRepository workspaceRepo;

    @Autowired
    private PostgresDigitalTwinRepositoryAdapter twinAdapter;

    @Autowired
    private PostgresInventoryLedgerRepositoryAdapter inventoryAdapter;

    @Test
    @DisplayName("Persists and loads digital twin state and inventory balances in PostgreSQL 16")
    void shouldPersistAndRetrieveDigitalTwinState() {
        WorkspaceId wsId = WorkspaceId.of("WS-PG-TEST-01");

        // 1. Save Workspace parent entity
        WorkspaceJpaEntity wsEntity = new WorkspaceJpaEntity(
                wsId.value(), "EV Battery Plant", "EV_BATTERY", Instant.now(), Instant.now());
        workspaceRepo.save(wsEntity);

        // 2. Persist DigitalTwinState via adapter
        DigitalTwinState state = new DigitalTwinState(wsId, StateVersion.of(5L), null, null);
        twinAdapter.save(state);

        assertThat(twinAdapter.existsByWorkspaceId(wsId)).isTrue();

        Optional<DigitalTwinState> retrieved = twinAdapter.findByWorkspaceId(wsId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCurrentVersion()).isEqualTo(StateVersion.of(5L));

        // 3. Persist and retrieve Inventory Balance via adapter
        InventoryAccountId accountId = InventoryAccountId.of(
                NodeId.of("WH-01"), MaterialId.of("RAW-LITHIUM"), BalanceCategory.ON_HAND_AVAILABLE);
        Quantity balanceQty = Quantity.of(new BigDecimal("1250.5000"), UnitOfMeasure.KILOGRAM);

        inventoryAdapter.updateBalance(wsId, accountId, balanceQty);

        Optional<Quantity> retrievedBalance = inventoryAdapter.findBalance(wsId, accountId);
        assertThat(retrievedBalance).isPresent();
        assertThat(retrievedBalance.get().amount()).isEqualByComparingTo("1250.5000");
        assertThat(retrievedBalance.get().unit()).isEqualTo(UnitOfMeasure.KILOGRAM);
    }
}

