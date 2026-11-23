package com.supplygraph.application.connector;

import com.supplygraph.application.outbox.InboxService;
import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static com.supplygraph.application.connector.WebhookIngestionService.Outcome.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebhookIngestionServiceTest {
    private final byte[] secret = "whsec".getBytes(StandardCharsets.UTF_8);
    private final byte[] body = "[{\"id\":\"1\"},{\"id\":\"2\"}]".getBytes(StandardCharsets.UTF_8);
    private Connector connector;
    private OutboxService outbox;
    private ConnectorExecutionRepositoryPort executions;
    private final Set<String> seen = new HashSet<>();
    private WebhookIngestionService service;

    @BeforeEach void setUp() {
        connector = Connector.register(WorkspaceId.of("ws"), "Hook", ConnectorCategory.TMS, ConnectorMode.LIVE, "rest")
                .configure(new ConnectorConfiguration(URI.create("https://tms.example"), new CredentialReference("env:HOOK"), List.of("tms.example"), 1000, 4096, 1)).activate();
        ConnectorRepositoryPort repo = mock(ConnectorRepositoryPort.class);
        when(repo.findById(connector.id())).thenReturn(Optional.of(connector));
        executions = mock(ConnectorExecutionRepositoryPort.class);
        outbox = mock(OutboxService.class);
        InboxService inbox = new InboxService((ws, c, id, now) -> seen.add(ws.value() + c + id));
        RecordPayloadInspector inspector = new RecordPayloadInspector() {
            public Inspection inspect(byte[] b) { if (b.length < 3) throw new com.supplygraph.domain.error.ValidationException("bad"); return new Inspection(2, 2); }
            public List<ExternalRecord> parse(byte[] b) { return List.of(); }
        };
        service = new WebhookIngestionService(repo, executions, ref -> Optional.of(secret), inspector, inbox, outbox);
    }

    private String ts() { return "" + Instant.now().getEpochSecond(); }
    private String sig(String ts, byte[] b) { return WebhookSignatureVerifier.sign(secret, Long.parseLong(ts), b); }

    @Test void validSignedDeliveryIsEnqueuedOnce() {
        String t = ts();
        assertThat(service.receive(connector.id().value(), t, sig(t, body), "d-1", body)).isEqualTo(ACCEPTED);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(outbox).enqueue(any(), eq(WebhookIngestionService.EVENT_TYPE), any(), any(), key.capture(), any());
        assertThat(key.getValue()).contains("d-1");
    }

    @Test void replayedDeliveryIsDuplicateWithNoSecondEffect() {
        String t = ts();
        service.receive(connector.id().value(), t, sig(t, body), "d-1", body);
        assertThat(service.receive(connector.id().value(), t, sig(t, body), "d-1", body)).isEqualTo(DUPLICATE);
        verify(outbox, times(1)).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test void replayWithoutDeliveryIdIsDedupedBySignature() {
        String t = ts();
        service.receive(connector.id().value(), t, sig(t, body), null, body);
        assertThat(service.receive(connector.id().value(), t, sig(t, body), null, body)).isEqualTo(DUPLICATE);
    }

    @Test void badSignatureStaleTimestampAndUnknownConnectorAreRejectedWithoutEffect() {
        String t = ts();
        assertThat(service.receive(connector.id().value(), t, "deadbeef", "d", body)).isEqualTo(REJECTED);
        String old = "" + (Instant.now().getEpochSecond() - 3600);
        assertThat(service.receive(connector.id().value(), old, sig(old, body), "d", body)).isEqualTo(REJECTED);
        assertThat(service.receive("CON-NOPE", t, sig(t, body), "d", body)).isEqualTo(REJECTED);
        verify(outbox, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test void oversizedOrMalformedPayloadRejected() {
        byte[] big = new byte[5000];
        String t = ts();
        assertThat(service.receive(connector.id().value(), t, sig(t, big), "d", big)).isEqualTo(REJECTED);
        byte[] tiny = "x".getBytes();
        assertThat(service.receive(connector.id().value(), t, sig(t, tiny), "d", tiny)).isEqualTo(REJECTED);
        verify(outbox, never()).enqueue(any(), any(), any(), any(), any(), any());
    }
}
