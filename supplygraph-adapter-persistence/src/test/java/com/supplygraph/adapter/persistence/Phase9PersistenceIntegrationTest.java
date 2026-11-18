package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.PostgresConnectorPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresOutboxPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresConnectorExecutionPersistenceAdapter;
import com.supplygraph.adapter.persistence.adapter.PostgresSyncPersistenceAdapter;
import com.supplygraph.domain.model.sync.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.outbox.BackoffPolicy;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = Phase9PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 9 PostgreSQL connector runtime / outbox / inbox integration test")
class Phase9PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_phase9_test").withUsername("test").withPassword("test");

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

    @Autowired PostgresOutboxPersistenceAdapter outbox;
    @Autowired PostgresConnectorPersistenceAdapter connectors;
    @Autowired PlatformTransactionManager txManager;
    @Autowired PostgresConnectorExecutionPersistenceAdapter executions;
    @Autowired PostgresSyncPersistenceAdapter sync;

    private OutboxEvent ev(String ws, String key) { return OutboxEvent.create(WorkspaceId.of(ws), "T", "agg", "{}", key, "corr"); }

    @Test void connectorRoundTripPersistsReferenceOnlyAndIsolatesWorkspaces() {
        WorkspaceId a = WorkspaceId.of("ws-conn-a"), b = WorkspaceId.of("ws-conn-b");
        ConnectorConfiguration cfg = new ConnectorConfiguration(java.net.URI.create("https://erp.example/api"),
                new CredentialReference("env:ERP_TOKEN"), List.of("erp.example"), 2000, 4096, 3);
        Connector live = Connector.register(a, "ERP", ConnectorCategory.ERP, ConnectorMode.LIVE, "rest").configure(cfg).activate();
        connectors.save(live);
        connectors.save(Connector.register(b, "Other", ConnectorCategory.WMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox"));

        Connector loaded = connectors.findById(live.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(ConnectorStatus.ACTIVE);
        assertThat(loaded.configuration().credential().value()).isEqualTo("env:ERP_TOKEN");
        assertThat(loaded.configuration().allowedHosts()).containsExactly("erp.example");
        assertThat(connectors.findByWorkspace(a)).extracting(Connector::name).containsExactly("ERP");
    }

    @Test void duplicateIdempotencyKeyYieldsSingleEvent() {
        OutboxEvent first = outbox.insertIfAbsent(ev("ws-dup", "k1"));
        OutboxEvent second = outbox.insertIfAbsent(ev("ws-dup", "k1"));
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(outbox.findByWorkspace(WorkspaceId.of("ws-dup"), null, 10)).hasSize(1);
        // same key in another workspace is independent
        outbox.insertIfAbsent(ev("ws-dup-2", "k1"));
        assertThat(outbox.findByWorkspace(WorkspaceId.of("ws-dup-2"), null, 10)).hasSize(1);
    }

    @Test void businessChangeAndOutboxRollBackTogether() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            outbox.insertIfAbsent(ev("ws-atomic", "k"));
            throw new IllegalStateException("business failure after enqueue");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(outbox.findByWorkspace(WorkspaceId.of("ws-atomic"), null, 10)).isEmpty();

        tx.executeWithoutResult(s -> outbox.insertIfAbsent(ev("ws-atomic", "k")));
        assertThat(outbox.findByWorkspace(WorkspaceId.of("ws-atomic"), null, 10)).hasSize(1);
    }

    @Test void concurrentClaimsNeverDeliverSameEventTwice() throws Exception {
        String ws = "ws-claim";
        for (int i = 0; i < 40; i++) outbox.insertIfAbsent(ev(ws, "k" + i));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<List<OutboxEvent>>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) futures.add(pool.submit(() -> outbox.claimDue(Instant.now().plusSeconds(1), 40)));
        Set<String> ids = new HashSet<>();
        int total = 0;
        for (Future<List<OutboxEvent>> f : futures) {
            for (OutboxEvent e : f.get(30, TimeUnit.SECONDS)) {
                if (e.workspaceId().value().equals(ws)) { total++; ids.add(e.id()); assertThat(e.status()).isEqualTo(OutboxStatus.IN_FLIGHT); }
            }
        }
        pool.shutdown();
        assertThat(total).isEqualTo(40);
        assertThat(ids).hasSize(40);
    }

    @Test void staleInFlightEventsRecoverAfterWorkerCrashAndDeadLetterReplays() {
        WorkspaceId ws = WorkspaceId.of("ws-recover");
        OutboxEvent e = outbox.insertIfAbsent(ev("ws-recover", "k"));
        List<OutboxEvent> claimed = outbox.claimDue(Instant.now().plusSeconds(1), 500).stream().filter(c -> c.id().equals(e.id())).toList();
        assertThat(claimed).hasSize(1);
        // worker "crashes"; nothing recovers a fresh in-flight event, but a stale one is recovered
        assertThat(outbox.recoverStale(Instant.now().minus(Duration.ofHours(1)), Instant.now())).isZero();
        assertThat(outbox.recoverStale(Instant.now().plusSeconds(60), Instant.now())).isGreaterThanOrEqualTo(1);
        assertThat(outbox.findById(e.id()).orElseThrow().status()).isEqualTo(OutboxStatus.PENDING);

        OutboxEvent again = outbox.claimDue(Instant.now().plusSeconds(1), 500).stream().filter(c -> c.id().equals(e.id())).findFirst().orElseThrow();
        outbox.save(again.markFailed("permanent", false, BackoffPolicy.standard(), Instant.now()));
        assertThat(outbox.countByStatus(ws).get(OutboxStatus.DEAD_LETTER)).isEqualTo(1L);
        outbox.save(outbox.findById(e.id()).orElseThrow().replay(Instant.now()));
        assertThat(outbox.findById(e.id()).orElseThrow().replayCount()).isEqualTo(1);
    }

    @Test void inboxSuppressesDuplicateDeliveryUnderConcurrency() throws Exception {
        WorkspaceId ws = WorkspaceId.of("ws-inbox");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 8; i++) results.add(pool.submit(() -> outbox.registerIfAbsent(ws, "consumer", "msg-1", Instant.now())));
        int firsts = 0;
        for (Future<Boolean> f : results) if (f.get(30, TimeUnit.SECONDS)) firsts++;
        pool.shutdown();
        assertThat(firsts).isEqualTo(1);
        assertThat(outbox.registerIfAbsent(ws, "consumer", "msg-2", Instant.now())).isTrue();
    }

    @Test void executionHistoryRetryLinkAndWorkspaceFilter() {
        Connector c = Connector.register(WorkspaceId.of("ws-exec"), "Sbx", ConnectorCategory.ERP, ConnectorMode.SANDBOX_SIMULATED, "sandbox");
        ConnectorExecution first = executions.save(ConnectorExecution.start(c, ConnectorOperation.SYNCHRONIZE, ExecutionTrigger.MANUAL, "u1", 1, null, "corr-1")
                .fail(ErrorCategory.TIMEOUT, "slow", true));
        assertThat(executions.hasRetry(first.id())).isFalse();
        ConnectorExecution retry = executions.save(ConnectorExecution.start(c, ConnectorOperation.SYNCHRONIZE, ExecutionTrigger.RETRY, "u1", 2, first.id(), "corr-1").succeed(5, 4, 1));
        assertThat(executions.hasRetry(first.id())).isTrue();
        ConnectorExecution loaded = executions.findById(retry.id()).orElseThrow();
        assertThat(loaded.simulated()).isTrue();
        assertThat(loaded.recordsRejected()).isEqualTo(1);
        assertThat(executions.findByConnector(c.id(), 10)).hasSize(2);
        assertThat(executions.findByWorkspace(WorkspaceId.of("someone-else"), 10)).isEmpty();
    }

    @Test void syncStateRoundTripAndConflictLookup() {
        WorkspaceId ws = WorkspaceId.of("ws-sync");
        ConnectorId cid = ConnectorId.of("CON-SYNC1");
        FieldMirror m = new FieldMirror(ws, cid, "SHIPMENT", "S1", "milestone", null, null, Instant.now()).withAgreedValue("DISPATCHED", Instant.now());
        sync.save(m);
        sync.save(sync.find(ws, cid, "SHIPMENT", "S1", "milestone").orElseThrow().withInternalValue("HELD", Instant.now()));
        FieldMirror loaded = sync.find(ws, cid, "SHIPMENT", "S1", "milestone").orElseThrow();
        assertThat(loaded.internalValue()).isEqualTo("HELD");
        assertThat(loaded.lastSyncedValue()).isEqualTo("DISPATCHED");
        assertThat(sync.find(WorkspaceId.of("other"), cid, "SHIPMENT", "S1", "milestone")).isEmpty();

        ReconciliationItem conflict = sync.save(ReconciliationItem.create(ws, cid, "SHIPMENT", "S1", "milestone", "DELIVERED", "HELD", "DISPATCHED",
                ReconciliationStatus.CONFLICT, "corr", false));
        assertThat(sync.existsOpenFor(ws, cid, "SHIPMENT", "S1", "milestone", "DELIVERED")).isTrue();
        assertThat(sync.findOpen(ws)).hasSize(1);
        assertThat(sync.findOpen(WorkspaceId.of("other"))).isEmpty();
        sync.save(conflict.resolve(true, "u1", "carrier proof", Instant.now()));
        assertThat(sync.findOpen(ws)).isEmpty();
        assertThat(sync.existsOpenFor(ws, cid, "SHIPMENT", "S1", "milestone", "DELIVERED")).isFalse();

        sync.save(new SyncCheckpoint(cid, null, null, 0).advance("c1", Instant.now()));
        assertThat(sync.find(cid).orElseThrow().syncCount()).isEqualTo(1);

        sync.save(new FieldAuthorityPolicy(ws, "SHIPMENT", "*", FieldAuthority.INTERNAL));
        sync.save(new FieldAuthorityPolicy(ws, "SHIPMENT", "*", FieldAuthority.EXTERNAL));  // upsert, not duplicate
        assertThat(sync.findByWorkspace(ws)).hasSize(1).first().extracting(FieldAuthorityPolicy::authority).isEqualTo(FieldAuthority.EXTERNAL);
    }
}
