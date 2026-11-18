package com.supplygraph.adapter.integration;

import com.supplygraph.application.execution.ExecutionAdapter;
import com.supplygraph.domain.model.execution.TaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIMULATED execution adapter. It contacts nothing. Every response it gives is a simulation: acknowledgements are NOT confirmations
 * from any real system. Behaviour is selected per task with the parameter {@code sim.behavior} so failure paths can be exercised deterministically:
 * ACK (default), CONFIRM, FAIL_RETRYABLE_ONCE, FAIL_RETRYABLE_ALWAYS, FAIL_PERMANENT, REJECT, AUTH_FAILURE, TIMEOUT, THROW.
 * Idempotent: a repeated idempotency key returns the original reference and never counts as a second execution.
 */
@Component
public class SandboxExecutionAdapter implements ExecutionAdapter {
    public static final String ID = "sandbox-execution";
    public static final String BEHAVIOR = "sim.behavior";

    private final Map<String, String> referencesByKey = new ConcurrentHashMap<>();
    private final Map<String, Integer> callsByKey = new ConcurrentHashMap<>();
    private final Map<String, Integer> executionsByKey = new ConcurrentHashMap<>();

    @Override public String adapterId() { return ID; }
    @Override public boolean simulated() { return true; }
    @Override public Set<TaskType> supportedTypes() { return EnumSet.allOf(TaskType.class); }
    @Override public Duration timeout() { return Duration.ofSeconds(2); }

    @Override
    public List<String> checkPreconditions(Request r) {
        List<String> violations = new ArrayList<>();
        String q = r.parameters().get("quantity");
        if (q != null) {
            try { if (new BigDecimal(q).signum() < 0) violations.add("quantity must not be negative"); }
            catch (NumberFormatException e) { violations.add("quantity is not a number"); }
        }
        return violations;
    }

    @Override
    public ExecutionResponse execute(Request r) {
        int call = callsByKey.merge(r.idempotencyKey(), 1, Integer::sum);
        String behavior = r.parameters().getOrDefault(BEHAVIOR, "ACK").toUpperCase();
        String ref = referencesByKey.computeIfAbsent(r.idempotencyKey(), k -> "SIM-" + Integer.toHexString(k.hashCode()).toUpperCase());
        switch (behavior) {
            case "FAIL_RETRYABLE_ONCE": if (call == 1) return ExecutionResponse.of(Outcome.RETRYABLE_ERROR, null, "[SIMULATED] transient failure"); break;
            case "FAIL_RETRYABLE_ALWAYS": return ExecutionResponse.of(Outcome.RETRYABLE_ERROR, null, "[SIMULATED] transient failure");
            case "FAIL_PERMANENT": return ExecutionResponse.of(Outcome.PERMANENT_ERROR, null, "[SIMULATED] permanent failure");
            case "REJECT": return ExecutionResponse.of(Outcome.REJECTED, null, "[SIMULATED] request rejected by target");
            case "AUTH_FAILURE": return ExecutionResponse.of(Outcome.AUTHENTICATION_FAILURE, null, "[SIMULATED] authentication failed");
            case "TIMEOUT": return ExecutionResponse.of(Outcome.TIMEOUT_UNKNOWN, null, "[SIMULATED] no response; outcome unknown");
            case "THROW": throw new IllegalStateException("[SIMULATED] adapter crashed");
            default: break;
        }
        executionsByKey.merge(r.idempotencyKey(), 1, Integer::sum);
        return "CONFIRM".equals(behavior)
                ? ExecutionResponse.of(Outcome.COMPLETED_CONFIRMED, ref, "[SIMULATED] completion confirmed")
                : ExecutionResponse.of(Outcome.ACKNOWLEDGED, ref, "[SIMULATED] request accepted (not a real-world confirmation)");
    }

    @Override
    public ExecutionResponse compensate(Request r) {
        return ExecutionResponse.of(Outcome.ACKNOWLEDGED, "SIM-COMP-" + r.taskId(), "[SIMULATED] compensation accepted");
    }

    /** Test/diagnostic: how many times the effect was actually "performed" for a key (idempotency check). */
    public int effectCount(String idempotencyKey) { return executionsByKey.getOrDefault(idempotencyKey, 0); }
}
