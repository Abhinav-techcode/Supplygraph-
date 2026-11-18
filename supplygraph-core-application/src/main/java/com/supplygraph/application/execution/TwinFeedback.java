package com.supplygraph.application.execution;

import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.event.*;
import com.supplygraph.domain.model.execution.ExecutionEvidence;
import com.supplygraph.domain.model.execution.ExecutionTask;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Feeds VALIDATED, non-simulated execution evidence into the existing operational event pipeline (Phase 5 monitoring).
 * Sending a request, or receiving a simulated acknowledgement, never reaches the digital twin.
 */
@Component
public class TwinFeedback {
    private final OperationalEventIngestionService events;

    public TwinFeedback(OperationalEventIngestionService events) { this.events = events; }

    /** @return true if an event was emitted. */
    public boolean recordVerifiedOutcome(ExecutionWorkflow w, ExecutionTask task, ExecutionEvidence evidence) {
        if (evidence.simulated() || w.simulated()) return false;
        Map<String, String> payload = new HashMap<>(evidence.observed());
        payload.put("workflowId", w.id());
        payload.put("taskId", task.id());
        payload.put("taskType", task.type().name());
        payload.put("evidenceKind", evidence.kind().name());
        payload.put("correlationId", w.correlationId());
        String target = task.parameters().getOrDefault("targetNodeId", task.id());
        boolean manual = evidence.kind() == ExecutionEvidence.Kind.MANUAL_CONFIRMATION;
        events.ingestEvent(OperationalEvent.createIngested(EventType.RECOVERY_ACTION_COMPLETED,
                manual ? EventSource.MANUAL_OPERATOR : EventSource.EXTERNAL_INTEGRATION, Instant.now(), target, "RECOVERY_TASK", w.workspaceId(), payload,
                new EventProvenance("SUPPLYGRAPH_EXECUTION", evidence.recordedBy(), w.id(), evidence.reference(), Instant.now()),
                "exec:" + task.id() + ":" + evidence.kind() + ":" + (evidence.reference() == null ? "none" : evidence.reference())));
        return true;
    }
}
