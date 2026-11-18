package com.supplygraph.domain.model.execution;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Proof (or the lack of it) about a task outcome. Simulated evidence never updates the digital twin. */
public record ExecutionEvidence(String id, String workflowId, String taskId, Kind kind, boolean simulated, String source, String reference,
                                Map<String, String> expected, Map<String, String> observed, String note, String recordedBy, Instant recordedAt) {
    public enum Kind { ADAPTER_ACKNOWLEDGEMENT, ADAPTER_CONFIRMATION, EXTERNAL_ACKNOWLEDGEMENT, OBSERVED_STATE, MANUAL_CONFIRMATION, MANUAL_NOT_EXECUTED }

    public ExecutionEvidence {
        expected = expected == null ? Map.of() : Map.copyOf(expected);
        observed = observed == null ? Map.of() : Map.copyOf(observed);
    }

    public static ExecutionEvidence of(String workflowId, String taskId, Kind kind, boolean simulated, String source, String reference,
                                       Map<String, String> expected, Map<String, String> observed, String note, String by) {
        return new ExecutionEvidence(UUID.randomUUID().toString(), workflowId, taskId, kind, simulated, source, reference, expected, observed, note, by, Instant.now());
    }
}
