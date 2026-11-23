package com.supplygraph.application.connector;

import com.supplygraph.domain.error.InvalidStateException;
import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.security.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/** Runs connector operations through the registered adapter and records every attempt. */
@Service
public class ConnectorExecutionService {
    private static final Logger log = LoggerFactory.getLogger(ConnectorExecutionService.class);

    private final ConnectorRepositoryPort connectors;
    private final ConnectorExecutionRepositoryPort executions;
    private final WorkspaceAccessGuard guard;
    private final AuditGovernanceService audit;
    private final SyncReconciliationService sync;
    private final Map<String, ConnectorAdapter> adapters = new HashMap<>();

    public ConnectorExecutionService(ConnectorRepositoryPort connectors, ConnectorExecutionRepositoryPort executions,
                                     WorkspaceAccessGuard guard, AuditGovernanceService audit, SyncReconciliationService sync, List<ConnectorAdapter> adapterList) {
        this.sync = sync;
        this.connectors = connectors;
        this.executions = executions;
        this.guard = guard;
        this.audit = audit;
        for (ConnectorAdapter a : adapterList) {
            if (adapters.put(a.adapterType(), a) != null) throw new ConflictException("Duplicate adapter type " + a.adapterType());
        }
    }

    public ConnectorExecution testConnectivity(ConnectorId id) { return run(id, ConnectorOperation.TEST_CONNECTIVITY, ExecutionTrigger.MANUAL, 1, null); }

    public ConnectorExecution synchronize(ConnectorId id) { return run(id, ConnectorOperation.SYNCHRONIZE, ExecutionTrigger.MANUAL, 1, null); }

    /** Retries a retryable failure once, within the connector's retry budget. Unknown/permanent outcomes are never retried. */
    public ConnectorExecution retry(String executionId) {
        ConnectorExecution prior = executions.findById(executionId).orElseThrow(() -> new ResourceNotFoundException("Execution not found: " + executionId));
        Connector c = connectors.findById(prior.connectorId()).orElseThrow(() -> new ResourceNotFoundException("Connector not found"));
        guard.enforceWorkspaceAndPermission(c.workspaceId(), Permission.CONNECTOR_EXECUTE);
        int budget = c.configuration() != null ? c.configuration().maxRetries() : 0;
        if (!prior.canRetry(budget)) throw new InvalidStateException("Execution is not eligible for retry (status " + prior.status() + ", attempt " + prior.attempt() + ", budget " + budget + ")");
        if (executions.hasRetry(executionId)) throw new ConflictException("Execution already retried");
        ConnectorExecution result = run(c, prior.operation(), ExecutionTrigger.RETRY, prior.attempt() + 1, executionId, prior.correlationId());
        audit(c, AuditAction.CONNECTOR_EXECUTION_RETRIED, result, "Retry of " + executionId);
        return result;
    }

    public List<ConnectorExecution> history(ConnectorId id, int limit) {
        Connector c = connectors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Connector not found: " + id));
        guard.enforceWorkspaceAccess(c.workspaceId());
        return executions.findByConnector(id, limit);
    }

    public List<ConnectorExecution> workspaceHistory(WorkspaceId ws, int limit) {
        guard.enforceWorkspaceAccess(ws);
        return executions.findByWorkspace(ws, limit);
    }

    private ConnectorExecution run(ConnectorId id, ConnectorOperation op, ExecutionTrigger trigger, int attempt, String retryOf) {
        Connector c = connectors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Connector not found: " + id));
        guard.enforceWorkspaceAndPermission(c.workspaceId(), Permission.CONNECTOR_EXECUTE);
        ConnectorExecution result = run(c, op, trigger, attempt, retryOf, null);
        audit(c, AuditAction.CONNECTOR_EXECUTED, result, op + " -> " + result.status());
        return result;
    }

    private ConnectorExecution run(Connector c, ConnectorOperation op, ExecutionTrigger trigger, int attempt, String retryOf, String correlationId) {
        String user = SecurityContext.getCurrentUser().id().value();
        boolean usable = op == ConnectorOperation.TEST_CONNECTIVITY
                ? (c.status() == ConnectorStatus.ACTIVE || c.status() == ConnectorStatus.CONFIGURED)
                : c.status() == ConnectorStatus.ACTIVE;
        ConnectorExecution running = executions.save(ConnectorExecution.start(c, op, trigger, user, attempt, retryOf, correlationId));
        if (!usable) {
            return executions.save(running.fail(ErrorCategory.POLICY_BLOCKED, "Connector is " + c.status() + "; operation " + op + " not allowed", false));
        }
        ConnectorAdapter adapter = adapters.get(c.adapterType());
        if (adapter == null) {
            return executions.save(running.fail(ErrorCategory.POLICY_BLOCKED, "No adapter registered for type '" + c.adapterType() + "'", false));
        }
        try {
            ConnectorCallResult r = op == ConnectorOperation.TEST_CONNECTIVITY ? adapter.testConnectivity(c) : adapter.synchronize(c);
            if (r.success() && op == ConnectorOperation.SYNCHRONIZE && !r.records().isEmpty()) {
                sync.apply(c, r.records(), running.correlationId());
            }
            return executions.save(r.success()
                    ? running.succeed(r.recordsReceived(), r.recordsProcessed(), r.recordsRejected())
                    : running.fail(r.errorCategory() != null ? r.errorCategory() : ErrorCategory.UNKNOWN, r.message(), r.retryable()));
        } catch (RuntimeException e) {
            log.error("Adapter {} threw unexpectedly for connector {} correlation={}", c.adapterType(), c.id(), running.correlationId(), e);
            return executions.save(running.fail(ErrorCategory.UNKNOWN, "Adapter failure: " + e.getClass().getSimpleName(), false));
        }
    }

    private void audit(Connector c, AuditAction action, ConnectorExecution e, String rationale) {
        audit.logAction(SecurityContext.getCurrentUser().id(), c.workspaceId(), action, e.id(), "NONE", e.status().name(),
                rationale + (e.simulated() ? " [SIMULATED]" : ""), null);
    }
}
