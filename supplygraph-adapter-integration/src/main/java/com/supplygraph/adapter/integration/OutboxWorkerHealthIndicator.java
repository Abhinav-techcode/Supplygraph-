package com.supplygraph.adapter.integration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** UP when the worker is running and has completed a run recently; reports DOWN when it is running but stalled. Unknown when the worker is disabled. */
@Component("outboxWorker")
public class OutboxWorkerHealthIndicator implements HealthIndicator {
    private static final Duration STALL_AFTER = Duration.ofMinutes(2);
    private final ObjectProvider<OutboxDispatcherWorker> worker;

    public OutboxWorkerHealthIndicator(ObjectProvider<OutboxDispatcherWorker> worker) { this.worker = worker; }

    @Override
    public Health health() {
        OutboxDispatcherWorker w = worker.getIfAvailable();
        if (w == null) return Health.unknown().withDetail("reason", "outbox worker disabled").build();
        if (!w.isRunning()) return Health.down().withDetail("reason", "worker stopped").build();
        long last = w.lastRunEpochMillis();
        Health.Builder b = last == 0 ? Health.up().withDetail("lastRun", "none yet")
                : Duration.between(Instant.ofEpochMilli(last), Instant.now()).compareTo(STALL_AFTER) > 0
                ? Health.down().withDetail("reason", "no successful run within " + STALL_AFTER) : Health.up();
        return b.withDetail("delivered", w.deliveredTotal()).withDetail("failedRuns", w.failedRuns()).build();
    }
}
