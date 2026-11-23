package com.supplygraph.application.connector;

import com.supplygraph.domain.error.InvalidStateException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConnectorExecutionServiceTest {
    private final WorkspaceId ws = WorkspaceId.of("ws-1");
    private ConnectorRepositoryPort connectors;
    private ConnectorExecutionRepositoryPort executions;
    private final Map<String, ConnectorExecution> store = new LinkedHashMap<>();
    private ConnectorCallResult next = ConnectorCallResult.ok(2, 2, 0, "ok");
    private int adapterCalls;
    private final SyncReconciliationService syncMock = mock(SyncReconciliationService.class);
    private Connector connector;
    private ConnectorExecutionService service;

    @BeforeEach void setUp() {
        connectors = mock(ConnectorRepositoryPort.class);
        executions = mock(ConnectorExecutionRepositoryPort.class);
        when(executions.save(any())).thenAnswer(i -> { ConnectorExecution e = i.getArgument(0); store.put(e.id(), e); return e; });
        when(executions.findById(any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<String>getArgument(0))));
        when(executions.hasRetry(any())).thenAnswer(i -> store.values().stream().anyMatch(e -> i.getArgument(0).equals(e.retryOf())));
        connector = Connector.register(ws, "Sbx", ConnectorCategory.ERP, ConnectorMode.SANDBOX_SIMULATED, "fake")
                .configure(new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 2)).activate();
        when(connectors.findById(connector.id())).thenReturn(Optional.of(connector));
        ConnectorAdapter adapter = new ConnectorAdapter() {
            public String adapterType() { return "fake"; }
            public ConnectorCallResult testConnectivity(Connector c) { adapterCalls++; return next; }
            public ConnectorCallResult synchronize(Connector c) { adapterCalls++; return next; }
        };
        service = new ConnectorExecutionService(connectors, executions, new WorkspaceAccessGuard(), mock(AuditGovernanceService.class), syncMock, List.of(adapter));
        login(UserRole.SUPPLY_CHAIN_MANAGER, ws);
    }
    @AfterEach void tearDown() { SecurityContext.clear(); }
    private void login(UserRole r, WorkspaceId w) { SecurityContext.setCurrentUser(UserProfile.create("U1", "u", "u@x.io", "U", r, Set.of(w))); }

    @Test void successfulSyncRecordsCountsAndSimulatedFlag() {
        ConnectorExecution e = service.synchronize(connector.id());
        assertThat(e.status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(e.recordsReceived()).isEqualTo(2);
        assertThat(e.simulated()).isTrue();
        assertThat(e.finishedAt()).isNotNull();
    }

    @Test void retryableFailureCanBeRetriedOnceWithinBudget() {
        next = ConnectorCallResult.failure(ErrorCategory.TIMEOUT, "slow", true);
        ConnectorExecution first = service.synchronize(connector.id());
        assertThat(first.status()).isEqualTo(ExecutionStatus.FAILED_RETRYABLE);
        next = ConnectorCallResult.ok(1, 1, 0, "ok");
        ConnectorExecution retried = service.retry(first.id());
        assertThat(retried.status()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(retried.attempt()).isEqualTo(2);
        assertThat(retried.retryOf()).isEqualTo(first.id());
        assertThat(retried.correlationId()).isEqualTo(first.correlationId());
        assertThatThrownBy(() -> service.retry(first.id())).hasMessageContaining("already retried");
    }

    @Test void permanentFailureIsNeverRetried() {
        next = ConnectorCallResult.failure(ErrorCategory.AUTHENTICATION, "denied", false);
        ConnectorExecution e = service.synchronize(connector.id());
        assertThatThrownBy(() -> service.retry(e.id())).isInstanceOf(InvalidStateException.class);
    }

    @Test void retryBudgetIsBounded() {
        next = ConnectorCallResult.failure(ErrorCategory.TIMEOUT, "slow", true);
        ConnectorExecution e = service.synchronize(connector.id());
        e = service.retry(e.id());   // attempt 2 (budget 2)
        e = service.retry(e.id());   // attempt 3
        ConnectorExecution last = e;
        assertThat(last.attempt()).isEqualTo(3);
        assertThatThrownBy(() -> service.retry(last.id())).hasMessageContaining("not eligible");
    }

    @Test void unexpectedAdapterExceptionBecomesUnknownPermanentFailure() {
        ConnectorAdapter boom = new ConnectorAdapter() {
            public String adapterType() { return "boom"; }
            public ConnectorCallResult testConnectivity(Connector c) { throw new IllegalStateException("x"); }
            public ConnectorCallResult synchronize(Connector c) { throw new IllegalStateException("x"); }
        };
        Connector c = Connector.register(ws, "B", ConnectorCategory.WMS, ConnectorMode.SANDBOX_SIMULATED, "boom")
                .configure(new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 2)).activate();
        when(connectors.findById(c.id())).thenReturn(Optional.of(c));
        ConnectorExecutionService s = new ConnectorExecutionService(connectors, executions, new WorkspaceAccessGuard(), mock(AuditGovernanceService.class), syncMock, List.of(boom));
        ConnectorExecution e = s.synchronize(c.id());
        assertThat(e.status()).isEqualTo(ExecutionStatus.FAILED_PERMANENT);
        assertThat(e.errorCategory()).isEqualTo(ErrorCategory.UNKNOWN);
    }

    @Test void inactiveConnectorCannotSynchronize() {
        Connector inactive = connector.deactivate();
        when(connectors.findById(connector.id())).thenReturn(Optional.of(inactive));
        ConnectorExecution e = service.synchronize(connector.id());
        assertThat(e.errorCategory()).isEqualTo(ErrorCategory.POLICY_BLOCKED);
        assertThat(adapterCalls).isZero();
    }

    @Test void unauthorizedAndCrossWorkspaceExecutionIsDenied() {
        login(UserRole.ANALYST, ws);
        assertThatThrownBy(() -> service.synchronize(connector.id())).isInstanceOf(UnauthorizedOperationException.class);
        login(UserRole.WORKSPACE_ADMIN, WorkspaceId.of("elsewhere"));
        assertThatThrownBy(() -> service.synchronize(connector.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> service.history(connector.id(), 10)).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThat(adapterCalls).isZero();
        verify(executions, never()).save(any());
    }
}
