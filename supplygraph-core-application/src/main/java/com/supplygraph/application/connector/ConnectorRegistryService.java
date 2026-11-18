package com.supplygraph.application.connector;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.security.Permission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/** Connector lifecycle with server-side workspace isolation, permission checks and audit. */
@Service
public class ConnectorRegistryService {
    private final ConnectorRepositoryPort repository;
    private final WorkspaceAccessGuard guard;
    private final AuditGovernanceService audit;

    public ConnectorRegistryService(ConnectorRepositoryPort repository, WorkspaceAccessGuard guard, AuditGovernanceService audit) {
        this.repository = repository;
        this.guard = guard;
        this.audit = audit;
    }

    public Connector register(WorkspaceId ws, String name, ConnectorCategory category, ConnectorMode mode, String adapterType) {
        guard.enforceWorkspaceAndPermission(ws, Permission.CONNECTOR_MANAGE);
        Connector saved = repository.save(Connector.register(ws, name, category, mode, adapterType));
        record(saved, AuditAction.CONNECTOR_REGISTERED, "NONE", "Registered " + mode + " connector '" + name + "'");
        return saved;
    }

    public Connector configure(ConnectorId id, ConnectorConfiguration cfg) {
        Connector c = load(id, Permission.CONNECTOR_MANAGE);
        Connector saved = repository.save(c.configure(cfg));
        record(saved, AuditAction.CONNECTOR_CONFIGURED, c.status().name(), "Configuration updated (credential reference only; no secret stored)");
        return saved;
    }

    public Connector activate(ConnectorId id) {
        Connector c = load(id, Permission.CONNECTOR_MANAGE);
        Connector saved = repository.save(c.activate());
        record(saved, AuditAction.CONNECTOR_ACTIVATED, c.status().name(), "Activated");
        return saved;
    }

    public Connector deactivate(ConnectorId id) {
        Connector c = load(id, Permission.CONNECTOR_MANAGE);
        Connector saved = repository.save(c.deactivate());
        record(saved, AuditAction.CONNECTOR_DEACTIVATED, c.status().name(), "Deactivated");
        return saved;
    }

    public Connector get(ConnectorId id) { return load(id, null); }

    public List<Connector> list(WorkspaceId ws) {
        guard.enforceWorkspaceAccess(ws);
        return repository.findByWorkspace(ws);
    }

    /** Loads then enforces the connector's own workspace, so a guessed ID from another workspace is refused (IDOR). */
    private Connector load(ConnectorId id, Permission permission) {
        Connector c = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Connector not found: " + id));
        if (permission != null) guard.enforceWorkspaceAndPermission(c.workspaceId(), permission);
        else guard.enforceWorkspaceAccess(c.workspaceId());
        return c;
    }

    private void record(Connector c, AuditAction action, String previous, String rationale) {
        audit.logAction(SecurityContext.getCurrentUser().id(), c.workspaceId(), action, c.id().value(), previous, c.status().name(), rationale, null);
    }
}
