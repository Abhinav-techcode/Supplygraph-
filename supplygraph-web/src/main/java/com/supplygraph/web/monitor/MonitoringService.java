package com.supplygraph.web.monitor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Watches the running application: health checks with thresholds, JVM and request statistics, a short history for charts and business counters.
 * Everything is measured here (no external monitoring system needed); the numbers are rolling and reset when the application restarts.
 */
@Service
public class MonitoringService {
    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final int MAX_SAMPLES = 120;      // 10 minutes at one sample per 5 seconds

    public enum Level { OK, WARN, DOWN }

    public record Check(String name, Level level, String detail) {}
    public record Sample(Instant at, double heapUsedMb, double heapMaxMb, int threads, double cpuPercent, double avgLatencyMs, double dbMs, int requests) {}
    public record Business(long facilities, long lanes, long activeIncidents, long agentRuns, long blockedWorkflows, long outboxPending, long outboxDead, long users) {}
    public record Requests(int last5Min, double avgMs, double p95Ms, int serverErrors, int clientErrors, Map<String, Integer> byArea) {}
    public record Snapshot(Instant at, Duration uptime, Level overall, List<Check> checks, Requests requests, Business business, List<Sample> samples,
                           int activeSessions, List<RecentErrors.Entry> errors, String javaVersion, int processors) {}

    private record Req(Instant at, long millis, int status, String area) {}

    private final JdbcTemplate jdbc;
    private final Instant started = Instant.now();
    private final Deque<Req> requests = new ConcurrentLinkedDeque<>();
    private final Deque<Sample> samples = new ConcurrentLinkedDeque<>();
    private final AtomicInteger sessions = new AtomicInteger();
    private ScheduledExecutorService sampler;

    public MonitoringService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void start() {
        sampler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "sg-monitor-sampler"); t.setDaemon(true); return t; });
        sampler.scheduleAtFixedRate(() -> {
            try { takeSample(); } catch (RuntimeException e) { log.warn("Monitoring sample failed: {}", e.toString()); }
        }, 1, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() { if (sampler != null) sampler.shutdownNow(); }

    public void sessionCreated() { sessions.incrementAndGet(); }
    public void sessionDestroyed() { sessions.updateAndGet(v -> Math.max(0, v - 1)); }

    public void recordRequest(String area, long millis, int status) {
        requests.addFirst(new Req(Instant.now(), millis, status, area));
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(15));
        while (requests.size() > 5000 || (!requests.isEmpty() && requests.peekLast().at().isBefore(cutoff))) requests.pollLast();
    }

    private void takeSample() {
        Runtime rt = Runtime.getRuntime();
        double used = (rt.totalMemory() - rt.freeMemory()) / 1048576.0, max = rt.maxMemory() / 1048576.0;
        double cpu = 0;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) cpu = Math.max(0, os.getProcessCpuLoad()) * 100;
        Instant from = Instant.now().minusSeconds(5);
        List<Req> recent = requests.stream().filter(r -> r.at().isAfter(from)).toList();
        double avg = recent.stream().mapToLong(Req::millis).average().orElse(0);
        samples.addLast(new Sample(Instant.now(), used, max, ManagementFactory.getThreadMXBean().getThreadCount(), cpu, avg, probeDatabase(), recent.size()));
        while (samples.size() > MAX_SAMPLES) samples.pollFirst();
    }

    /** Round-trip time of a trivial query in ms; negative when the database does not answer. */
    private double probeDatabase() {
        long t0 = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return (System.nanoTime() - t0) / 1_000_000.0;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public Snapshot snapshot() {
        Instant now = Instant.now();
        Instant window = now.minus(Duration.ofMinutes(5));
        List<Req> recent = requests.stream().filter(r -> r.at().isAfter(window)).toList();
        double[] times = recent.stream().mapToLong(Req::millis).sorted().asDoubleStream().toArray();
        double p95 = times.length == 0 ? 0 : times[(int) Math.min(times.length - 1, Math.floor(times.length * 0.95))];
        Map<String, Integer> byArea = new TreeMap<>();
        recent.forEach(r -> byArea.merge(r.area(), 1, Integer::sum));
        Requests rq = new Requests(recent.size(), recent.stream().mapToLong(Req::millis).average().orElse(0), p95,
                (int) recent.stream().filter(r -> r.status() >= 500).count(), (int) recent.stream().filter(r -> r.status() >= 400 && r.status() < 500).count(), byArea);

        List<Check> checks = new ArrayList<>();
        checks.add(new Check("Application", Level.OK, "Running for " + human(Duration.between(started, now)) + " on Java " + System.getProperty("java.version")));
        double db = probeDatabase();
        checks.add(db < 0 ? new Check("Database", Level.DOWN, "The database does not answer queries.")
                : new Check("Database", db < 100 ? Level.OK : db < 500 ? Level.WARN : Level.DOWN, String.format("Answers in %.0f ms", db)));
        Runtime rt = Runtime.getRuntime();
        double usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576.0, maxMb = rt.maxMemory() / 1048576.0, pct = 100 * usedMb / maxMb;
        checks.add(new Check("Memory", pct < 75 ? Level.OK : pct < 90 ? Level.WARN : Level.DOWN, String.format("%.0f of %.0f MB heap in use (%.0f%%)", usedMb, maxMb, pct)));
        File disk = new File(".");
        double freePct = disk.getTotalSpace() == 0 ? 100 : 100.0 * disk.getUsableSpace() / disk.getTotalSpace();
        double freeGb = disk.getUsableSpace() / 1.0737e9;
        // Percentages mislead on very large disks, so the absolute space counts too: DOWN only when almost nothing is left.
        checks.add(new Check("Disk", freeGb < 2 || freePct < 1 ? Level.DOWN : freePct < 15 && freeGb < 20 ? Level.WARN : Level.OK, String.format("%.0f%% free (%.1f GB)", freePct, freeGb)));
        checks.add(new Check("Response time", rq.last5Min() == 0 ? Level.OK : rq.p95Ms() < 800 ? Level.OK : rq.p95Ms() < 2000 ? Level.WARN : Level.DOWN,
                rq.last5Min() == 0 ? "No requests in the last 5 minutes" : String.format("95%% of %d requests answered within %.0f ms", rq.last5Min(), rq.p95Ms())));
        long errs = RecentErrors.countSince(window);
        checks.add(new Check("Errors", rq.serverErrors() == 0 && errs <= 2 ? Level.OK : errs <= 10 ? Level.WARN : Level.DOWN,
                errs + " error(s) and " + rq.serverErrors() + " server error response(s) in the last 5 minutes"));
        Business b = business();
        checks.add(new Check("Outbox", b.outboxDead() > 0 ? Level.WARN : b.outboxPending() > 100 ? Level.WARN : Level.OK,
                b.outboxPending() + " waiting, " + b.outboxDead() + " dead-lettered"));
        checks.add(new Check("Digital twin", b.facilities() > 0 ? Level.OK : Level.WARN, b.facilities() > 0 ? b.facilities() + " facilities, " + b.lanes() + " lanes loaded" : "No supply chain loaded yet"));
        Level overall = checks.stream().anyMatch(c -> c.level() == Level.DOWN) ? Level.DOWN : checks.stream().anyMatch(c -> c.level() == Level.WARN) ? Level.WARN : Level.OK;
        return new Snapshot(now, Duration.between(started, now), overall, checks, rq, b, new ArrayList<>(samples), sessions.get(), RecentErrors.latest(15),
                System.getProperty("java.version"), Runtime.getRuntime().availableProcessors());
    }

    private Business business() {
        return new Business(count("SELECT COUNT(*) FROM supply_chain_nodes"), count("SELECT COUNT(*) FROM transportation_lanes"),
                count("SELECT COUNT(*) FROM disruptions WHERE status = 'ACTIVE'"), count("SELECT COUNT(*) FROM agent_investigations"),
                count("SELECT COUNT(*) FROM execution_workflows WHERE status = 'BLOCKED'"), count("SELECT COUNT(*) FROM outbox_events WHERE status IN ('PENDING','IN_FLIGHT')"),
                count("SELECT COUNT(*) FROM outbox_events WHERE status = 'DEAD_LETTER'"), count("SELECT COUNT(*) FROM user_profiles"));
    }

    private long count(String sql) {
        try {
            Long n = jdbc.queryForObject(sql, Long.class);
            return n == null ? 0 : n;
        } catch (RuntimeException e) {
            log.debug("Monitoring query failed ({}): {}", sql, e.getMessage());
            return 0;
        }
    }

    static String human(Duration d) {
        long h = d.toHours(), m = d.toMinutesPart(), s = d.toSecondsPart();
        return h > 0 ? h + " h " + m + " min" : m > 0 ? m + " min " + s + " s" : s + " s";
    }
}
