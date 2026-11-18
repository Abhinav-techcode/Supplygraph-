package com.supplygraph.adapter.integration;

import com.sun.net.httpserver.HttpServer;
import com.supplygraph.application.connector.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Genuine HTTP round trips against a local in-process test server (explicitly a local integration server, not a vendor system). */
class RestConnectorAdapterTest {
    private HttpServer server;
    private final AtomicReference<String> seenAuth = new AtomicReference<>();
    private volatile int healthStatus = 200;
    private volatile int sleepMillis = 0;
    private volatile String recordsBody = "[{\"id\":\"PO-1\",\"entityType\":\"PURCHASE_ORDER\",\"fields\":{\"status\":\"OPEN\"}},{\"id\":\"PO-2\",\"entityType\":\"PURCHASE_ORDER\",\"fields\":{\"status\":\"SHIPPED\"}},{\"nope\":1}]";
    private RestConnectorAdapter adapter;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/health", ex -> {
            seenAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            if (sleepMillis > 0) try { Thread.sleep(sleepMillis); } catch (InterruptedException ignored) {}
            ex.sendResponseHeaders(healthStatus, -1); ex.close();
        });
        server.createContext("/api/records", ex -> {
            byte[] b = recordsBody.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length); ex.getResponseBody().write(b); ex.close();
        });
        server.createContext("/api/redirect", ex -> { ex.getResponseHeaders().add("Location", "http://localhost/evil"); ex.sendResponseHeaders(302, -1); ex.close(); });
        server.start();
        CredentialResolver creds = ref -> Optional.of("tok-123".getBytes(StandardCharsets.UTF_8));
        adapter = new RestConnectorAdapter(creds, new JsonRecordPayloadInspector(), new OutboundUrlGuard(true));
    }
    @AfterEach void stop() { server.stop(0); }

    private Connector connector(String path, int timeout, long maxBytes) {
        URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + path);
        return Connector.register(WorkspaceId.of("ws"), "Local", ConnectorCategory.ERP, ConnectorMode.LIVE, "rest")
                .configure(new ConnectorConfiguration(uri, new CredentialReference("env:TEST_TOKEN"), List.of("localhost"), timeout, maxBytes, 3)).activate();
    }

    @Test void connectivitySendsBearerFromCredentialReference() {
        ConnectorCallResult r = adapter.testConnectivity(connector("/api", 2000, 1024));
        assertThat(r.success()).isTrue();
        assertThat(seenAuth.get()).isEqualTo("Bearer tok-123");
    }

    @Test void synchronizeCountsReceivedValidAndRejectedRecords() {
        ConnectorCallResult r = adapter.synchronize(connector("/api", 2000, 4096));
        assertThat(r.success()).isTrue();
        assertThat(r.recordsReceived()).isEqualTo(3);
        assertThat(r.recordsProcessed()).isEqualTo(2);
        assertThat(r.recordsRejected()).isEqualTo(1);
        assertThat(r.records()).extracting(com.supplygraph.application.connector.ExternalRecord::id).containsExactly("PO-1", "PO-2");
    }

    @Test void authenticationFailureIsPermanent() {
        healthStatus = 401;
        ConnectorCallResult r = adapter.testConnectivity(connector("/api", 2000, 1024));
        assertThat(r.errorCategory()).isEqualTo(ErrorCategory.AUTHENTICATION);
        assertThat(r.retryable()).isFalse();
    }

    @Test void serverErrorIsRetryable() {
        healthStatus = 503;
        ConnectorCallResult r = adapter.testConnectivity(connector("/api", 2000, 1024));
        assertThat(r.errorCategory()).isEqualTo(ErrorCategory.REJECTED_BY_REMOTE);
        assertThat(r.retryable()).isTrue();
    }

    @Test void timeoutIsReportedAndRetryable() {
        sleepMillis = 1500;
        ConnectorCallResult r = adapter.testConnectivity(connector("/api", 200, 1024));
        assertThat(r.errorCategory()).isEqualTo(ErrorCategory.TIMEOUT);
        assertThat(r.retryable()).isTrue();
    }

    @Test void oversizedResponseIsRejected() {
        recordsBody = "[" + "{\"id\":\"A\"},".repeat(500) + "{\"id\":\"B\"}]";
        ConnectorCallResult r = adapter.synchronize(connector("/api", 2000, 100));
        assertThat(r.errorCategory()).isEqualTo(ErrorCategory.RESPONSE_TOO_LARGE);
    }

    @Test void malformedPayloadIsInvalidResponse() {
        recordsBody = "not json";
        assertThat(adapter.synchronize(connector("/api", 2000, 1024)).errorCategory()).isEqualTo(ErrorCategory.INVALID_RESPONSE);
    }

    @Test void redirectsAreNotFollowed() {
        ConnectorCallResult r = adapter.testConnectivity(connector("/api/redirect/x", 2000, 1024));
        assertThat(r.success()).isFalse();
    }

    @Test void productionGuardBlocksLoopbackTarget() {
        RestConnectorAdapter strict = new RestConnectorAdapter(ref -> Optional.of(new byte[]{1}), new JsonRecordPayloadInspector());
        ConnectorCallResult r = strict.testConnectivity(connector("/api", 2000, 1024));
        assertThat(r.errorCategory()).isEqualTo(ErrorCategory.POLICY_BLOCKED);
    }

    @Test void unresolvedCredentialIsAuthenticationFailure() {
        RestConnectorAdapter noCred = new RestConnectorAdapter(ref -> Optional.empty(), new JsonRecordPayloadInspector(), new OutboundUrlGuard(true));
        assertThat(noCred.testConnectivity(connector("/api", 2000, 1024)).errorCategory()).isEqualTo(ErrorCategory.AUTHENTICATION);
    }

    @Test void simulatedConnectorsAreRefused() {
        Connector sim = Connector.register(WorkspaceId.of("ws"), "S", ConnectorCategory.WMS, ConnectorMode.SANDBOX_SIMULATED, "rest");
        assertThat(adapter.testConnectivity(sim.configure(new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 1))).success()).isFalse();
    }
}
