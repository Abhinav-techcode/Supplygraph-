package com.supplygraph.adapter.integration;

import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SandboxAndWorkerTest {
    private Connector connector(ConnectorMode mode) {
        return Connector.register(WorkspaceId.of("ws"), "C", ConnectorCategory.TMS, mode, "sandbox")
                .configure(new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 1));
    }

    @Test void sandboxIsLabelledSimulatedAndDeterministic() {
        SandboxConnectorAdapter a = new SandboxConnectorAdapter();
        var r = a.synchronize(connector(ConnectorMode.SANDBOX_SIMULATED));
        assertThat(r.success()).isTrue();
        assertThat(r.message()).startsWith("[SIMULATED]");
        assertThat(r.recordsReceived()).isEqualTo(SandboxConnectorAdapter.SIMULATED_RECORDS);
        assertThat(a.synchronize(connector(ConnectorMode.SANDBOX_SIMULATED)).records()).isEqualTo(r.records()).hasSize(5);
    }

    @Test void sandboxRefusesLiveConnector() {
        Connector live = Connector.register(WorkspaceId.of("ws"), "L", ConnectorCategory.ERP, ConnectorMode.LIVE, "sandbox");
        assertThat(new SandboxConnectorAdapter().testConnectivity(live).success()).isFalse();
    }

    @Test void inspectorValidatesStructure() {
        var i = new JsonRecordPayloadInspector().inspect("{\"records\":[{\"id\":\"a\",\"entityType\":\"STOCK\",\"fields\":{\"onHandQuantity\":10}},{\"id\":\"\"},{\"id\":\"b\",\"entityType\":\"STOCK\",\"fields\":{}},5]}".getBytes());
        assertThat(i.received()).isEqualTo(4);
        assertThat(i.valid()).isEqualTo(1);
        assertThat(new JsonRecordPayloadInspector().parse("[{\"id\":\"a\",\"entityType\":\"STOCK\",\"fields\":{\"onHandQuantity\":10,\"x\":null}}]".getBytes()).get(0).fields())
                .containsExactly(java.util.Map.entry("onHandQuantity", "10"));
    }

    @Test void workerSurvivesDispatchFailuresAndStopsGracefully() throws Exception {
        OutboxService outbox = mock(OutboxService.class);
        when(outbox.dispatchBatch(anyInt())).thenThrow(new IllegalStateException("db down")).thenReturn(3);
        OutboxDispatcherWorker w = new OutboxDispatcherWorker(outbox, 60_000, 10);
        w.start();
        assertThat(w.isRunning()).isTrue();
        w.runOnce(); w.runOnce();
        assertThat(w.failedRuns()).isEqualTo(1);
        assertThat(w.deliveredTotal()).isEqualTo(3);
        w.stop();
        assertThat(w.isRunning()).isFalse();
    }
}
