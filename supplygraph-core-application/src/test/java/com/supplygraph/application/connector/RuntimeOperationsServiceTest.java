package com.supplygraph.application.connector;

import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuntimeOperationsServiceTest {
    private final WorkspaceId ws = WorkspaceId.of("ws");
    @AfterEach void tearDown() { SecurityContext.clear(); }

    @Test void indicatorsAreDerivedOnlyFromRecordedData() {
        Connector c = Connector.register(ws, "C", ConnectorCategory.ERP, ConnectorMode.SANDBOX_SIMULATED, "sandbox");
        ConnectorExecution ok = ConnectorExecution.start(c, ConnectorOperation.SYNCHRONIZE, ExecutionTrigger.MANUAL, "u", 1, null, null).succeed(10, 8, 2);
        ConnectorExecution bad = ConnectorExecution.start(c, ConnectorOperation.SYNCHRONIZE, ExecutionTrigger.RETRY, "u", 2, "x", null).fail(ErrorCategory.TIMEOUT, "t", true);
        ConnectorRepositoryPort cr = mock(ConnectorRepositoryPort.class);
        ConnectorExecutionRepositoryPort er = mock(ConnectorExecutionRepositoryPort.class);
        OutboxRepositoryPort or = mock(OutboxRepositoryPort.class);
        when(cr.findByWorkspace(ws)).thenReturn(List.of(c));
        when(er.findByWorkspace(eq(ws), anyInt())).thenReturn(List.of(ok, bad));
        when(er.findByConnector(eq(c.id()), anyInt())).thenReturn(List.of(bad, ok));
        when(or.countByStatus(ws)).thenReturn(Map.of(OutboxStatus.DELIVERED, 3L, OutboxStatus.DEAD_LETTER, 1L));
        when(or.findByWorkspace(eq(ws), eq(OutboxStatus.DELIVERED), anyInt())).thenReturn(List.of());
        SecurityContext.setCurrentUser(UserProfile.create("U", "u", "u@x.io", "U", UserRole.ANALYST, Set.of(ws)));

        var s = new RuntimeOperationsService(cr, er, or, new WorkspaceAccessGuard()).snapshot(ws);
        assertThat(s.succeeded()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(1);
        assertThat(s.importRejectionRate()).isEqualTo(0.2);
        assertThat(s.errorCategories()).containsEntry(ErrorCategory.TIMEOUT, 1L);
        assertThat(s.totalRetries()).isEqualTo(1);
        assertThat(s.eventFailureRate()).isEqualTo(0.25);
        assertThat(s.averageDeliveryLatency()).isNull();   // nothing delivered in sample => unknown, not zero
        assertThat(s.connectors().get(0).lastSucceeded()).isFalse();
    }

    @Test void emptyWorkspaceYieldsUnknownRatesAndRespectsIsolation() {
        ConnectorRepositoryPort cr = mock(ConnectorRepositoryPort.class);
        ConnectorExecutionRepositoryPort er = mock(ConnectorExecutionRepositoryPort.class);
        OutboxRepositoryPort or = mock(OutboxRepositoryPort.class);
        when(or.countByStatus(any())).thenReturn(Map.of());
        SecurityContext.setCurrentUser(UserProfile.create("U", "u", "u@x.io", "U", UserRole.ANALYST, Set.of(ws)));
        RuntimeOperationsService svc = new RuntimeOperationsService(cr, er, or, new WorkspaceAccessGuard());
        var s = svc.snapshot(ws);
        assertThat(s.importRejectionRate()).isNull();
        assertThat(s.averageSyncLatency()).isNull();
        assertThat(s.eventFailureRate()).isNull();
        assertThatThrownBy(() -> svc.snapshot(WorkspaceId.of("other"))).isInstanceOf(WorkspaceAccessDeniedException.class);
    }
}
