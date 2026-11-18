package com.supplygraph.application.execution;

import com.supplygraph.application.execution.ExecutionAdapter.ExecutionResponse;
import com.supplygraph.application.execution.ExecutionAdapter.Outcome;
import com.supplygraph.application.outbox.InboxService;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic, seeded evaluation scenarios. Execution mode: SIMULATED adapter, in-memory ports (software-correctness evidence only).
 * Results are written to target/research/execution-scenarios.json with seed, trial counts, failures and summary statistics.
 * These scenarios show invariants held across generated cases; they do NOT demonstrate superiority over any other system, and the latency
 * figures measure in-process software overhead, not real enterprise-system latency.
 */
class ExecutionResearchScenariosTest {
    private static final long SEED = 20261118L;
    @AfterEach void tearDown() { SecurityContext.clear(); }

    record Result(String id, String description, String executionMode, int trials, int invariantViolations, Map<String, Double> stats) {}

    @Test void runScenariosAndRecordResults() throws IOException {
        List<Result> results = List.of(dependencyEnforcement(), duplicateSuppression(), approvalEnforcement(), unknownOutcomeNeverRetried(), idempotentRetries(), replanningBounded());
        write(results);
        for (Result r : results) assertThat(r.invariantViolations()).as(r.id()).isZero();
    }

    // S1: random DAGs with seeded failures -> a task is never attempted unless all predecessors COMPLETED; failed predecessors always block dependents.
    private Result dependencyEnforcement() {
        Random rnd = new Random(SEED);
        int trials = 200, violations = 0;
        List<Double> latenciesMs = new ArrayList<>();
        for (int t = 0; t < trials; t++) {
            ExecutionFixture f = new ExecutionFixture(true);
            int n = 3 + rnd.nextInt(6);
            List<ExecutionTask> tasks = new ArrayList<>();
            String wid = "WR-" + t;
            for (int i = 1; i <= n; i++) {
                List<String> deps = new ArrayList<>();
                for (int j = 1; j < i; j++) if (rnd.nextDouble() < 0.35) deps.add(wid + "-T" + j);
                tasks.add(ExecutionTask.create(wid, i, TaskType.values()[rnd.nextInt(TaskType.values().length)], "t" + i, Map.of(), deps, Map.of(), 2));
            }
            for (ExecutionTask task : tasks) if (rnd.nextDouble() < 0.2) f.recording.script.put(task.id(), ExecutionResponse.of(Outcome.PERMANENT_ERROR, null, "seeded failure"));
            ExecutionWorkflow w = readyCustom(f, tasks);
            long start = System.nanoTime();
            f.as(UserRole.LOGISTICS_MANAGER);
            w = f.orchestration.start(w.id());
            f.as(UserRole.SUPPLY_CHAIN_MANAGER);
            for (int guard = 0; guard < 50; guard++) {                         // deliver acknowledgements until quiescent
                w = f.workflows.get(wid);
                Optional<ExecutionTask> waiting = w.tasks().stream().filter(x -> x.status() == TaskStatus.AWAITING_ACK).findFirst();
                if (waiting.isEmpty()) break;
                f.acks.record(waiting.get().id(), "ACK-" + waiting.get().id(), true, "scenario", null);
            }
            latenciesMs.add((System.nanoTime() - start) / 1e6);
            w = f.workflows.get(wid);
            Map<String, ExecutionTask> byId = new HashMap<>();
            w.tasks().forEach(x -> byId.put(x.id(), x));
            for (ExecutionTask x : w.tasks()) {
                for (String d : x.dependsOn()) {
                    TaskStatus ds = byId.get(d).status();
                    if (x.attempts() > 0 && ds != TaskStatus.COMPLETED && ds != TaskStatus.COMPENSATED) violations++;           // ran before/without predecessor completion
                    if (ds == TaskStatus.FAILED && x.status() != TaskStatus.BLOCKED && x.status() != TaskStatus.CANCELLED) violations++; // failure not propagated
                }
            }
        }
        return new Result("S1-dependency-enforcement", "Random DAGs (3-8 tasks) with 20% seeded permanent failures; acks delivered sequentially", "SIMULATED adapter, in-memory ports", trials, violations, stats(latenciesMs, "workflowRunMillis"));
    }

    // S2: duplicate delivery: every message is delivered 1-5 times in shuffled order -> exactly one effect per message.
    private Result duplicateSuppression() {
        Random rnd = new Random(SEED + 1);
        int trials = 200, violations = 0, totalDeliveries = 0, suppressed = 0;
        for (int t = 0; t < trials; t++) {
            Set<String> seen = new HashSet<>();
            InboxService inbox = new InboxService((ws, c, id, now) -> seen.add(ws.value() + c + id));
            int messages = 1 + rnd.nextInt(20);
            List<Integer> deliveries = new ArrayList<>();
            for (int m = 0; m < messages; m++) for (int k = 0, copies = 1 + rnd.nextInt(5); k < copies; k++) deliveries.add(m);
            Collections.shuffle(deliveries, rnd);
            Map<Integer, AtomicInteger> effects = new HashMap<>();
            for (int m : deliveries) { totalDeliveries++; if (!inbox.processOnce(com.supplygraph.domain.model.WorkspaceId.of("ws"), "c", "m" + m, () -> effects.computeIfAbsent(m, k -> new AtomicInteger()).incrementAndGet())) suppressed++; }
            for (int m = 0; m < messages; m++) if (effects.getOrDefault(m, new AtomicInteger()).get() != 1) violations++;
        }
        return new Result("S2-duplicate-suppression", "Each message delivered 1-5 times in shuffled order (inbox with in-memory port; PostgreSQL concurrency is covered by Phase9PersistenceIntegrationTest)", "unit-level", trials, violations,
                Map.of("totalDeliveries", (double) totalDeliveries, "suppressedDuplicates", (double) suppressed));
    }

    // S3: every role x every consequential action; only roles holding the permission may succeed, SoD holds.
    private Result approvalEnforcement() {
        int checks = 0, violations = 0;
        for (UserRole role : UserRole.values()) {
            ExecutionFixture f = new ExecutionFixture(true);
            var plan = f.approvedPlan("u-supply_chain_manager");
            f.as(UserRole.SUPPLY_CHAIN_MANAGER);
            ExecutionWorkflow w = f.conversion.convert(plan.id().value());
            f.as(role);
            checks++;
            boolean mayApprove = role.hasPermission(Permission.EXECUTION_APPROVE) && !role.name().equals("SUPPLY_CHAIN_MANAGER");   // manager is the creator -> SoD
            boolean approved;
            try { f.approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, "scenario"); approved = true; } catch (RuntimeException e) { approved = false; }
            if (approved != mayApprove) violations++;
            checks++;
            boolean mayStart = false;   // workflow is not READY unless a sensitive second approval exists: nobody may start it here
            boolean started;
            try { f.orchestration.start(w.id()); started = true; } catch (RuntimeException e) { started = false; }
            if (started != mayStart) violations++;
        }
        return new Result("S3-approval-enforcement", "Every role (9) attempts approval and start on a PENDING_APPROVAL workflow created by a SUPPLY_CHAIN_MANAGER", "SIMULATED adapter, in-memory ports", checks, violations, Map.of("rolesTested", (double) UserRole.values().length));
    }

    // S4: unknown outcomes must never be sent twice without human reconciliation.
    private Result unknownOutcomeNeverRetried() {
        int trials = 100, violations = 0;
        for (int t = 0; t < trials; t++) {
            ExecutionFixture f = new ExecutionFixture(true);
            ExecutionWorkflow w = f.readyWorkflow();
            f.recording.script.put(w.tasks().get(0).id(), ExecutionResponse.of(Outcome.TIMEOUT_UNKNOWN, null, "seeded timeout"));
            f.as(UserRole.LOGISTICS_MANAGER);
            w = f.orchestration.start(w.id());
            String id = w.id(), first = w.tasks().get(0).id();
            try { f.orchestration.retryTask(id, first); violations++; } catch (IllegalStateException expected) { /* required */ }
            long sends = f.recording.calls.stream().filter(c -> c.taskId().equals(first)).count();
            if (sends != 1) violations++;
        }
        return new Result("S4-unknown-outcome-never-retried", "First task times out with unknown outcome; automatic retry attempts must be refused", "SIMULATED adapter, in-memory ports", trials, violations, Map.of());
    }

    // S5: retries of a retryable failure reuse one idempotency key and never exceed the attempt budget.
    private Result idempotentRetries() {
        Random rnd = new Random(SEED + 4);
        int trials = 100, violations = 0;
        for (int t = 0; t < trials; t++) {
            ExecutionFixture f = new ExecutionFixture(true);
            ExecutionWorkflow w = f.readyWorkflow();
            String first = w.tasks().get(0).id(), wid = w.id();
            f.recording.script.put(first, ExecutionResponse.of(Outcome.RETRYABLE_ERROR, null, "503"));
            f.as(UserRole.LOGISTICS_MANAGER);
            f.orchestration.start(wid);
            int retries = 0;
            for (int i = 0; i < 6 + rnd.nextInt(3); i++) { try { f.orchestration.retryTask(wid, first); retries++; } catch (IllegalStateException e) { break; } }
            long keys = f.recording.calls.stream().filter(c -> c.taskId().equals(first)).map(ExecutionAdapter.Request::idempotencyKey).distinct().count();
            long sends = f.recording.calls.stream().filter(c -> c.taskId().equals(first)).count();
            if (keys != 1 || sends > 3) violations++;
        }
        return new Result("S5-idempotent-bounded-retries", "Always-failing retryable task: one idempotency key, at most maxAttempts (3) sends", "SIMULATED adapter, in-memory ports", trials, violations, Map.of());
    }

    // S6: replanning proposals are bounded by depth, suppressed when a proposal is open, and never raised for unknown/simulated data.
    private Result replanningBounded() {
        Random rnd = new Random(SEED + 5);
        int trials = 150, violations = 0, proposed = 0;
        for (int t = 0; t < trials; t++) {
            ExecutionFixture f = new ExecutionFixture(false);
            int depth = rnd.nextInt(4);
            f.policyStore.put(f.ws, new ExecutionPolicy(f.ws, true, true, true, true, 3, 2, 15));
            ExecutionWorkflow w = f.readyWorkflow();
            f.workflows.put(w.id(), new ExecutionWorkflow(w.id(), w.workspaceId(), w.planId(), w.correlationId(), w.status(), w.createdBy(), w.planApprovedBy(), w.sourceTwinVersion(), false, depth, null, w.tasks(), w.approvals(), w.createdAt(), w.updatedAt(), w.version() + 1));
            f.as(UserRole.SUPPLY_CHAIN_MANAGER);
            double actual = 1000 * (0.5 + rnd.nextDouble());                    // -50% .. +50%
            f.closedLoop.recordActual(w.id(), OutcomeMetric.TOTAL_COST, actual, "scenario", false);
            boolean material = Math.abs(actual - 1000) / 1000 * 100 >= 15;
            Optional<ReassessmentRequest> r1 = f.closedLoop.evaluateDeviation(w.id());
            Optional<ReassessmentRequest> r2 = f.closedLoop.evaluateDeviation(w.id());
            boolean expected = material && depth < 2;
            if (r1.isPresent() != expected) violations++;
            if (r2.isPresent()) violations++;                                  // never a duplicate
            if (r1.isPresent()) proposed++;
            if (r1.isPresent() && r1.get().status() != ReassessmentRequest.Status.PROPOSED) violations++;
        }
        return new Result("S6-bounded-replanning", "Random measured cost deviations (-50..+50%), random depth 0-3, limit 2, threshold 15%", "LIVE-mode workflow semantics with simulated adapter, in-memory ports", trials, violations, Map.of("proposalsRaised", (double) proposed));
    }

    private ExecutionWorkflow readyCustom(ExecutionFixture f, List<ExecutionTask> tasks) {
        ExecutionWorkflow w = ExecutionWorkflow.draft(f.ws, "PLAN-R", "u-supply_chain_manager", "u-supply_chain_manager", f.twinVersion.toString(), tasks, null, 0).validate().submitForApproval();
        w = w.addApproval(new WorkflowApproval(w.id(), "u-workspace_admin", "WORKSPACE_ADMIN", WorkflowApproval.Scope.WORKFLOW, true, "ok", Instant.now()));
        w = w.addApproval(new WorkflowApproval(w.id(), "u-system_admin", "SYSTEM_ADMIN", WorkflowApproval.Scope.SENSITIVE_TASKS, true, "ok", Instant.now()));
        w = w.markApproved(true).markReady(true);
        f.workflows.put(w.id(), w);
        return w;
    }

    private static Map<String, Double> stats(List<Double> xs, String name) {
        double mean = xs.stream().mapToDouble(d -> d).average().orElse(0);
        double sd = Math.sqrt(xs.stream().mapToDouble(d -> (d - mean) * (d - mean)).sum() / Math.max(1, xs.size() - 1));
        List<Double> sorted = xs.stream().sorted().toList();
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(name + "Mean", mean); m.put(name + "StdDev", sd); m.put(name + "P50", sorted.get(sorted.size() / 2)); m.put(name + "P95", sorted.get((int) (sorted.size() * 0.95)));
        return m;
    }

    private static String git(String... args) {
        try {
            List<String> cmd = new ArrayList<>(List.of("git"));
            cmd.addAll(List.of(args));
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return p.waitFor() == 0 ? out : "unknown";
        } catch (Exception e) { return "unknown"; }
    }

    private void write(List<Result> results) throws IOException {
        StringBuilder sb = new StringBuilder("{\n  \"softwareVersion\": \"0.1.0-SNAPSHOT\",\n  \"gitCommit\": \"" + git("rev-parse", "HEAD") + "\",\n  \"gitDirtyFiles\": "
                + git("status", "--porcelain").lines().count() + ",\n  \"seed\": " + SEED + ",\n  \"digitalTwinSnapshot\": \"in-memory StateVersion.initial()\",\n  \"generatedAt\": \"" + Instant.now() + "\",\n  \"scenarios\": [\n");
        for (int i = 0; i < results.size(); i++) {
            Result r = results.get(i);
            sb.append("    {\"id\": \"").append(r.id()).append("\", \"description\": \"").append(r.description()).append("\", \"executionMode\": \"").append(r.executionMode())
                    .append("\", \"connectorMode\": \"none (no connector involved)\", \"trials\": ").append(r.trials()).append(", \"invariantViolations\": ").append(r.invariantViolations())
                    .append(", \"stats\": {");
            int k = 0;
            for (var e : r.stats().entrySet()) sb.append(k++ > 0 ? ", " : "").append('"').append(e.getKey()).append("\": ").append(String.format(Locale.ROOT, "%.3f", e.getValue()));
            sb.append("}}").append(i < results.size() - 1 ? "," : "").append('\n');
        }
        sb.append("  ]\n}\n");
        Path out = Path.of("target", "research", "execution-scenarios.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }
}
