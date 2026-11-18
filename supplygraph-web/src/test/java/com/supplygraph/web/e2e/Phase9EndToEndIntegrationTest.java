package com.supplygraph.web.e2e;

import com.supplygraph.application.connector.*;
import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.port.out.FieldMirrorRepositoryPort;
import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.event.EventSource;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.domain.model.security.*;
import com.supplygraph.domain.model.sync.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Whole Phase 9 runtime wired through Spring against real PostgreSQL: connectors, sync, reconciliation, outbox/inbox, webhooks, replay, isolation. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase9EndToEndIntegrationTest {

    static final byte[] SECRET = "e2e-webhook-secret".getBytes(StandardCharsets.UTF_8);

    @TestConfiguration
    static class Config {
        @Bean @Primary CredentialResolver testCredentials() { return ref -> Optional.of(SECRET); }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("e2e").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired ConnectorRegistryService registry;
    @Autowired ConnectorExecutionService executions;
    @Autowired SyncReconciliationService sync;
    @Autowired OutboxService outbox;
    @Autowired OutboxRepositoryPort outboxRepo;
    @Autowired WebhookIngestionService webhooks;
    @Autowired FieldMirrorRepositoryPort mirrors;
    @Autowired ConnectorRepositoryPort connectorRepo;
    @Autowired OperationalEventIngestionService eventService;
    @Autowired TransactionTemplate tx;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @BeforeEach void seedWorkspaces() {
        for (String id : List.of("ws-e2e", "ws-e2e-other")) {
            jdbc.update("INSERT INTO workspaces (id, name, industry, created_at, updated_at) VALUES (?, ?, 'EV_BATTERY', now(), now()) ON CONFLICT DO NOTHING", id, id);
        }
    }

    static final WorkspaceId WS = WorkspaceId.of("ws-e2e");
    static final WorkspaceId OTHER = WorkspaceId.of("ws-e2e-other");
    static Connector sandbox;
    static Connector live;

    private static void as(UserRole role, WorkspaceId... wss) {
        SecurityContext.setCurrentUser(UserProfile.create("e2e-" + role, "u", "u@x.io", "U", role, Set.of(wss)));
    }
    @AfterEach void clear() { SecurityContext.clear(); }

    @Test @Order(1) void sandboxConnectorLifecycleAndSimulatedSyncFeedsMonitoringPipeline() {
        as(UserRole.SUPPLY_CHAIN_MANAGER, WS);
        sandbox = registry.register(WS, "Sandbox TMS", ConnectorCategory.TMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox");
        sandbox = registry.configure(sandbox.id(), new ConnectorConfiguration(null, null, List.of(), 1000, 4096, 2));
        sandbox = registry.activate(sandbox.id());

        ConnectorExecution test = executions.testConnectivity(sandbox.id());
        assertThat(test.status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        ConnectorExecution first = executions.synchronize(sandbox.id());
        assertThat(first.status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(first.simulated()).isTrue();
        assertThat(first.recordsReceived()).isEqualTo(5);

        List<OperationalEvent> events = eventService.getWorkspaceEvents(WS, 100);
        assertThat(events).hasSize(10); // 5 records x (milestone + delayHours)
        assertThat(events).allSatisfy(e -> assertThat(e.getSource()).isEqualTo(EventSource.SYNTHETIC_SCENARIO));
        assertThat(events).extracting(OperationalEvent::getEventType).contains(EventType.SHIPMENT_STATUS_CHANGED, EventType.SUPPLIER_DELIVERY_DELAYED);

        executions.synchronize(sandbox.id());                      // identical second sync: incremental, no change
        assertThat(eventService.getWorkspaceEvents(WS, 100)).hasSize(10);
        assertThat(sync.checkpoint(sandbox.id()).orElseThrow().syncCount()).isEqualTo(2);
    }

    @Test @Order(2) void divergenceBecomesConflictAndHumanResolvesIt() {
        as(UserRole.SUPPLY_CHAIN_MANAGER, WS);
        FieldMirror m = mirrors.find(WS, sandbox.id(), "SHIPMENT", "SIM-SHP-1", "milestone").orElseThrow();
        mirrors.save(m.withInternalValue("HELD_BY_OPERATOR", Instant.now()));
        var summary = sync.apply(sandbox, List.of(new ExternalRecord("SIM-SHP-1", "SHIPMENT", Map.of("milestone", "DELIVERED"))), "corr-e2e");
        assertThat(summary.conflicts()).isEqualTo(1);
        assertThat(mirrors.find(WS, sandbox.id(), "SHIPMENT", "SIM-SHP-1", "milestone").orElseThrow().internalValue()).isEqualTo("HELD_BY_OPERATOR");

        ReconciliationItem item = sync.openConflicts(WS).get(0);
        as(UserRole.ANALYST, WS);
        assertThatThrownBy(() -> sync.resolve(item.id(), true, "no")).isInstanceOf(UnauthorizedOperationException.class);
        as(UserRole.SUPPLY_CHAIN_MANAGER, OTHER);
        assertThatThrownBy(() -> sync.resolve(item.id(), true, "no")).isInstanceOf(WorkspaceAccessDeniedException.class);
        as(UserRole.SUPPLY_CHAIN_MANAGER, WS);
        assertThat(sync.resolve(item.id(), true, "carrier proof of delivery").status()).isEqualTo(ReconciliationStatus.ACCEPTED);
        assertThat(mirrors.find(WS, sandbox.id(), "SHIPMENT", "SIM-SHP-1", "milestone").orElseThrow().internalValue()).isEqualTo("DELIVERED");
        assertThat(sync.openConflicts(WS)).isEmpty();
    }

    @Test @Order(3) void signedWebhookIsDurableIdempotentAndAppliedExactlyOnce() throws Exception {
        as(UserRole.SUPPLY_CHAIN_MANAGER, WS);
        live = registry.register(WS, "Live TMS", ConnectorCategory.TMS, ConnectorMode.LIVE, "rest");
        live = registry.configure(live.id(), new ConnectorConfiguration(URI.create("https://tms.example/api"), new CredentialReference("env:TMS_WEBHOOK"),
                List.of("tms.example"), 1000, 8192, 2));
        live = registry.activate(live.id());
        SecurityContext.clear();

        byte[] body = "[{\"id\":\"LIVE-SHP-9\",\"entityType\":\"SHIPMENT\",\"fields\":{\"milestone\":\"DISPATCHED\"}}]".getBytes(StandardCharsets.UTF_8);
        String ts = Long.toString(Instant.now().getEpochSecond());
        String sig = WebhookSignatureVerifier.sign(SECRET, Long.parseLong(ts), body);

        assertThat(webhooks.receive(live.id().value(), ts, "00" + sig.substring(2), "d-1", body)).isEqualTo(WebhookIngestionService.Outcome.REJECTED);
        assertThat(webhooks.receive(live.id().value(), ts, sig, "d-1", body)).isEqualTo(WebhookIngestionService.Outcome.ACCEPTED);
        assertThat(webhooks.receive(live.id().value(), ts, sig, "d-1", body)).isEqualTo(WebhookIngestionService.Outcome.DUPLICATE);

        int eventsBefore = eventService.getWorkspaceEvents(WS, 500).size();
        assertThat(outbox.dispatchBatch(50)).isGreaterThanOrEqualTo(1);
        assertThat(mirrors.find(WS, live.id(), "SHIPMENT", "LIVE-SHP-9", "milestone").orElseThrow().internalValue()).isEqualTo("DISPATCHED");
        assertThat(eventService.getWorkspaceEvents(WS, 500)).hasSize(eventsBefore + 1);
        assertThat(eventService.getWorkspaceEvents(WS, 500)).anySatisfy(e -> assertThat(e.getSource()).isEqualTo(EventSource.EXTERNAL_INTEGRATION));

        // redelivery of the same outbox event to the handler has no second effect (inbox)
        OutboxEvent delivered = outboxRepo.findByWorkspace(WS, OutboxStatus.DELIVERED, 50).stream().filter(e -> e.aggregateId().equals(live.id().value())).findFirst().orElseThrow();
        outboxRepo.save(delivered);
        int before = eventService.getWorkspaceEvents(WS, 500).size();
        outbox.dispatchBatch(50);
        assertThat(eventService.getWorkspaceEvents(WS, 500)).hasSize(before);
    }

    @Test @Order(4) void httpEndpointAuthenticatesByHmacOnly() throws Exception {
        byte[] body = "[{\"id\":\"LIVE-SHP-10\",\"entityType\":\"SHIPMENT\",\"fields\":{\"milestone\":\"IN_TRANSIT\"}}]".getBytes(StandardCharsets.UTF_8);
        String ts = Long.toString(Instant.now().getEpochSecond());
        String sig = WebhookSignatureVerifier.sign(SECRET, Long.parseLong(ts), body);
        mvc.perform(post("/api/webhooks/" + live.id().value()).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-SG-Timestamp", ts).header("X-SG-Signature", sig).header("X-SG-Delivery", "http-1")).andExpect(status().isAccepted());
        mvc.perform(post("/api/webhooks/" + live.id().value()).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-SG-Timestamp", ts).header("X-SG-Signature", sig).header("X-SG-Delivery", "http-1")).andExpect(status().isAccepted());
        mvc.perform(post("/api/webhooks/" + live.id().value()).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-SG-Timestamp", ts).header("X-SG-Signature", "deadbeef")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/CON-DOESNOTEXIST").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-SG-Timestamp", ts).header("X-SG-Signature", sig)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/" + live.id().value()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
    }

    @Test @Order(5) void deadLetterCanBeInspectedAndReplayedOnlyByAuthorizedUsersInTheirWorkspace() {
        tx.executeWithoutResult(s -> outbox.enqueue(WS, "unknown.event.type", "agg-1", "{}", "dlq-key-1", "corr-dlq"));
        outbox.dispatchBatch(50);
        as(UserRole.SUPPLY_CHAIN_MANAGER, WS);
        List<OutboxEvent> dead = outbox.list(WS, OutboxStatus.DEAD_LETTER, 50);
        assertThat(dead).hasSize(1);
        assertThat(dead.get(0).lastError()).contains("No handler");
        String id = dead.get(0).id();

        as(UserRole.ANALYST, WS);
        assertThatThrownBy(() -> outbox.replay(id, "try")).isInstanceOf(UnauthorizedOperationException.class);
        as(UserRole.SUPPLY_CHAIN_MANAGER, OTHER);
        assertThatThrownBy(() -> outbox.replay(id, "idor")).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> outbox.list(WS, null, 10)).isInstanceOf(WorkspaceAccessDeniedException.class);

        as(UserRole.SUPPLY_CHAIN_MANAGER, WS);
        assertThat(outbox.replay(id, "handler fixed").status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxRepo.findById(id).orElseThrow().replayCount()).isEqualTo(1);
    }

    @Test @Order(6) void connectorAccessIsWorkspaceIsolated() {
        as(UserRole.WORKSPACE_ADMIN, OTHER);
        assertThatThrownBy(() -> registry.get(sandbox.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> executions.synchronize(sandbox.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> registry.list(WS)).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThat(registry.list(OTHER)).isEmpty();
    }

    @Test @Order(7) void credentialsAreNeverPersistedOrExposed() {
        Connector c = connectorRepo.findById(live.id()).orElseThrow();
        assertThat(c.configuration().credential().value()).isEqualTo("env:TMS_WEBHOOK");
        assertThat(c.toString() + c.configuration()).doesNotContain(new String(SECRET));
    }
}
