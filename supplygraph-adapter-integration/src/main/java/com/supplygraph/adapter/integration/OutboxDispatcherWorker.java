package com.supplygraph.adapter.integration;

import com.supplygraph.application.outbox.OutboxService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Single-threaded bounded dispatcher. Pending work survives restarts because it lives in PostgreSQL; shutdown waits for the in-progress batch. */
@Component
@ConditionalOnProperty(name = "supplygraph.outbox.worker.enabled", havingValue = "true")
public class OutboxDispatcherWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcherWorker.class);

    private final OutboxService outbox;
    private final long intervalMillis;
    private final int batchSize;
    private ScheduledExecutorService executor;
    private final AtomicLong lastRunEpochMillis = new AtomicLong();
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong failedRuns = new AtomicLong();

    public OutboxDispatcherWorker(OutboxService outbox,
                                  @Value("${supplygraph.outbox.worker.interval-millis:2000}") long intervalMillis,
                                  @Value("${supplygraph.outbox.worker.batch-size:50}") int batchSize) {
        this.outbox = outbox;
        this.intervalMillis = intervalMillis;
        this.batchSize = batchSize;
    }

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "outbox-dispatcher"); t.setDaemon(false); return t; });
        executor.scheduleWithFixedDelay(this::runOnce, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    /** Public for tests and the operations dashboard's manual trigger. Never throws. */
    public void runOnce() {
        try {
            delivered.addAndGet(outbox.dispatchBatch(batchSize));
            lastRunEpochMillis.set(Instant.now().toEpochMilli());
        } catch (RuntimeException e) { // e.g. database unavailable: keep the schedule alive, report via health
            failedRuns.incrementAndGet();
            log.error("Outbox dispatch run failed: {}", e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (executor == null) return;
        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) executor.shutdownNow();
    }

    public boolean isRunning() { return executor != null && !executor.isShutdown(); }
    public long lastRunEpochMillis() { return lastRunEpochMillis.get(); }
    public long deliveredTotal() { return delivered.get(); }
    public long failedRuns() { return failedRuns.get(); }
}
