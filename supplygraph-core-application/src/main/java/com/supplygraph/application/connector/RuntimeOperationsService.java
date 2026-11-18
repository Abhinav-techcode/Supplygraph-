package com.supplygraph.application.connector;

import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/** Derives operational indicators strictly from persisted executions and outbox rows; nothing is estimated or invented. */
@Service
public class RuntimeOperationsService {
    public record ConnectorAvailability(Connector connector, ConnectorExecution lastExecution, boolean lastSucceeded, int recentFailures) {}

    public record Snapshot(
            int sampledExecutions, int succeeded, int failed, long totalReceived, long totalProcessed, long totalRejected,
            Double importRejectionRate,            // null when nothing was received
            Duration averageSyncLatency,           // null when no finished executions
            Map<ErrorCategory, Long> errorCategories,
            int totalRetries,
            Map<OutboxStatus, Long> outboxCounts,
            Duration averageDeliveryLatency,       // null when nothing delivered
            Double eventFailureRate,               // dead-letter / (delivered + dead-letter); null when none finished
            List<ConnectorAvailability> connectors) {}

    private static final int SAMPLE = 500;
    private final ConnectorRepositoryPort connectors;
    private final ConnectorExecutionRepositoryPort executions;
    private final OutboxRepositoryPort outbox;
    private final WorkspaceAccessGuard guard;

    public RuntimeOperationsService(ConnectorRepositoryPort connectors, ConnectorExecutionRepositoryPort executions, OutboxRepositoryPort outbox, WorkspaceAccessGuard guard) {
        this.connectors = connectors; this.executions = executions; this.outbox = outbox; this.guard = guard;
    }

    public Snapshot snapshot(WorkspaceId ws) {
        guard.enforceWorkspaceAccess(ws);
        List<ConnectorExecution> runs = executions.findByWorkspace(ws, SAMPLE);
        int ok = 0, bad = 0, retries = 0;
        long recv = 0, proc = 0, rej = 0, latencyMs = 0; int finished = 0;
        Map<ErrorCategory, Long> cats = new EnumMap<>(ErrorCategory.class);
        for (ConnectorExecution e : runs) {
            if (e.status() == ExecutionStatus.SUCCEEDED) ok++;
            else if (e.status() != ExecutionStatus.RUNNING) bad++;
            if (e.trigger() == ExecutionTrigger.RETRY) retries++;
            recv += e.recordsReceived(); proc += e.recordsProcessed(); rej += e.recordsRejected();
            if (e.finishedAt() != null) { finished++; latencyMs += Duration.between(e.startedAt(), e.finishedAt()).toMillis(); }
            if (e.errorCategory() != null) cats.merge(e.errorCategory(), 1L, Long::sum);
        }
        Map<OutboxStatus, Long> counts = outbox.countByStatus(ws);
        List<OutboxEvent> delivered = outbox.findByWorkspace(ws, OutboxStatus.DELIVERED, 200);
        Duration deliveryLatency = delivered.isEmpty() ? null : Duration.ofMillis(
                (long) delivered.stream().mapToLong(d -> Duration.between(d.createdAt(), d.updatedAt()).toMillis()).average().orElse(0));
        long done = counts.getOrDefault(OutboxStatus.DELIVERED, 0L) + counts.getOrDefault(OutboxStatus.DEAD_LETTER, 0L);
        Double failureRate = done == 0 ? null : (double) counts.getOrDefault(OutboxStatus.DEAD_LETTER, 0L) / done;

        List<ConnectorAvailability> availability = new ArrayList<>();
        for (Connector c : connectors.findByWorkspace(ws)) {
            List<ConnectorExecution> mine = executions.findByConnector(c.id(), 10);
            ConnectorExecution last = mine.isEmpty() ? null : mine.get(0);
            int failures = (int) mine.stream().filter(m -> m.status() == ExecutionStatus.FAILED_PERMANENT || m.status() == ExecutionStatus.FAILED_RETRYABLE).count();
            availability.add(new ConnectorAvailability(c, last, last != null && last.status() == ExecutionStatus.SUCCEEDED, failures));
        }
        return new Snapshot(runs.size(), ok, bad, recv, proc, rej, recv == 0 ? null : (double) rej / recv,
                finished == 0 ? null : Duration.ofMillis(latencyMs / finished), cats, retries, counts, deliveryLatency, failureRate, availability);
    }
}
