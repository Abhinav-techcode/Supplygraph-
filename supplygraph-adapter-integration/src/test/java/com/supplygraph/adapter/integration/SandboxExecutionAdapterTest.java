package com.supplygraph.adapter.integration;

import com.supplygraph.application.execution.ExecutionAdapter.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.TaskType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxExecutionAdapterTest {
    private final SandboxExecutionAdapter adapter = new SandboxExecutionAdapter();
    private static Request req(String key, Map<String, String> params) { return new Request(WorkspaceId.of("ws"), "W", "W-T1", TaskType.ALLOCATE_INVENTORY, params, key, "corr", "u"); }

    @Test void isSimulatedSupportsAllTypesAndNeverClaimsRealConfirmation() {
        assertThat(adapter.simulated()).isTrue();
        assertThat(adapter.supportedTypes()).containsExactlyInAnyOrder(TaskType.values());
        ExecutionResponse r = adapter.execute(req("k1", Map.of()));
        assertThat(r.outcome()).isEqualTo(Outcome.ACKNOWLEDGED);
        assertThat(r.message()).contains("SIMULATED").contains("not a real-world confirmation");
    }

    @Test void idempotencyKeyReturnsSameReferenceAndNeverDoubleExecutes() {
        ExecutionResponse a = adapter.execute(req("k2", Map.of()));
        ExecutionResponse b = adapter.execute(req("k2", Map.of()));
        assertThat(b.externalReference()).isEqualTo(a.externalReference());
        assertThat(adapter.execute(req("k3", Map.of())).externalReference()).isNotEqualTo(a.externalReference());
    }

    @Test void behaviourSwitchesCoverTheFailureTaxonomy() {
        assertThat(adapter.execute(req("b1", Map.of("sim.behavior", "CONFIRM"))).outcome()).isEqualTo(Outcome.COMPLETED_CONFIRMED);
        assertThat(adapter.execute(req("b2", Map.of("sim.behavior", "FAIL_PERMANENT"))).outcome()).isEqualTo(Outcome.PERMANENT_ERROR);
        assertThat(adapter.execute(req("b3", Map.of("sim.behavior", "REJECT"))).outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(adapter.execute(req("b4", Map.of("sim.behavior", "AUTH_FAILURE"))).outcome()).isEqualTo(Outcome.AUTHENTICATION_FAILURE);
        assertThat(adapter.execute(req("b5", Map.of("sim.behavior", "TIMEOUT"))).outcome()).isEqualTo(Outcome.TIMEOUT_UNKNOWN);
        assertThat(adapter.execute(req("b6", Map.of("sim.behavior", "FAIL_RETRYABLE_ONCE"))).outcome()).isEqualTo(Outcome.RETRYABLE_ERROR);
        assertThat(adapter.execute(req("b6", Map.of("sim.behavior", "FAIL_RETRYABLE_ONCE"))).outcome()).isEqualTo(Outcome.ACKNOWLEDGED);
        assertThat(adapter.effectCount("b6")).isEqualTo(1);
    }

    @Test void preconditionsRejectInvalidQuantities() {
        assertThat(adapter.checkPreconditions(req("p", Map.of("quantity", "-1")))).isNotEmpty();
        assertThat(adapter.checkPreconditions(req("p", Map.of("quantity", "abc")))).isNotEmpty();
        assertThat(adapter.checkPreconditions(req("p", Map.of("quantity", "10")))).isEmpty();
    }

    @Test void compensationIsAlsoSimulated() {
        assertThat(adapter.compensate(req("c", Map.of())).message()).contains("SIMULATED");
    }
}
