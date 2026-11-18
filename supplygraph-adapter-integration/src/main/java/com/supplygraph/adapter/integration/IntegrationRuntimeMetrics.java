package com.supplygraph.adapter.integration;

import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Micrometer gauges backed by persisted outbox state and the dispatcher worker's own counters. Reads are cheap aggregate queries. */
@Component
public class IntegrationRuntimeMetrics implements MeterBinder {
    private final OutboxRepositoryPort outbox;
    private final ObjectProvider<OutboxDispatcherWorker> worker;

    public IntegrationRuntimeMetrics(OutboxRepositoryPort outbox, ObjectProvider<OutboxDispatcherWorker> worker) {
        this.outbox = outbox;
        this.worker = worker;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (OutboxStatus s : OutboxStatus.values()) {
            Gauge.builder("supplygraph.outbox.events", () -> safeCount(s)).tag("status", s.name().toLowerCase())
                    .description("Outbox events by status (all workspaces, aggregate only)").register(registry);
        }
        Gauge.builder("supplygraph.outbox.worker.delivered", () -> worker.stream().mapToLong(OutboxDispatcherWorker::deliveredTotal).sum()).register(registry);
        Gauge.builder("supplygraph.outbox.worker.failed.runs", () -> worker.stream().mapToLong(OutboxDispatcherWorker::failedRuns).sum()).register(registry);
        Gauge.builder("supplygraph.outbox.worker.running", () -> worker.stream().anyMatch(OutboxDispatcherWorker::isRunning) ? 1 : 0).register(registry);
    }

    private double safeCount(OutboxStatus s) {
        try { return outbox.countAllByStatus().getOrDefault(s, 0L); } catch (RuntimeException e) { return Double.NaN; }
    }
}
