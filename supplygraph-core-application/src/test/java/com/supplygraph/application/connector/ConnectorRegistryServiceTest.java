package com.supplygraph.application.connector;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConnectorRegistryServiceTest {
    private final WorkspaceId ws = WorkspaceId.of("ws-1");
    private ConnectorRepositoryPort repo;
    private AuditGovernanceService audit;
    private ConnectorRegistryService service;

    @BeforeEach void setUp() {
        repo = mock(ConnectorRepositoryPort.class);
        audit = mock(AuditGovernanceService.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new ConnectorRegistryService(repo, new WorkspaceAccessGuard(), audit);
    }
    @AfterEach void tearDown() { SecurityContext.clear(); }

    private void login(UserRole role, WorkspaceId w) {
        SecurityContext.setCurrentUser(UserProfile.create("U1", "u", "u@x.io", "U", role, Set.of(w)));
    }

    @Test void managerRunsFullLifecycleWithAudit() {
        login(UserRole.SUPPLY_CHAIN_MANAGER, ws);
        Connector c = service.register(ws, "Sandbox ERP", ConnectorCategory.ERP, ConnectorMode.SANDBOX_SIMULATED, "sandbox");
        when(repo.findById(c.id())).thenReturn(Optional.of(c));
        Connector cfg = service.configure(c.id(), new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 2));
        when(repo.findById(c.id())).thenReturn(Optional.of(cfg));
        assertThat(service.activate(c.id()).status()).isEqualTo(ConnectorStatus.ACTIVE);
        verify(audit).logAction(any(), eq(ws), eq(AuditAction.CONNECTOR_REGISTERED), eq(c.id().value()), any(), any(), any(), any());
        verify(audit).logAction(any(), eq(ws), eq(AuditAction.CONNECTOR_ACTIVATED), eq(c.id().value()), any(), any(), any(), any());
    }

    @Test void analystCannotRegister() {
        login(UserRole.ANALYST, ws);
        assertThatThrownBy(() -> service.register(ws, "x", ConnectorCategory.WMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox"))
                .isInstanceOf(UnauthorizedOperationException.class);
        verify(repo, never()).save(any());
    }

    @Test void crossWorkspaceConnectorAccessIsDenied() {
        Connector foreign = Connector.register(WorkspaceId.of("ws-other"), "Foreign", ConnectorCategory.TMS, ConnectorMode.SANDBOX_SIMULATED, "sandbox");
        when(repo.findById(foreign.id())).thenReturn(Optional.of(foreign));
        login(UserRole.WORKSPACE_ADMIN, ws);
        assertThatThrownBy(() -> service.get(foreign.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> service.deactivate(foreign.id())).isInstanceOf(WorkspaceAccessDeniedException.class);
        assertThatThrownBy(() -> service.list(WorkspaceId.of("ws-other"))).isInstanceOf(WorkspaceAccessDeniedException.class);
    }
}
