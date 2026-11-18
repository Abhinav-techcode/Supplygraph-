package com.supplygraph.application.execution;

import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.port.out.ExecutionAttemptRepositoryPort;
import com.supplygraph.domain.model.execution.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only advisory boundary for the bounded agents. Advisories are explanations derived from persisted facts, tagged with the agent role
 * they would be attributed to. They cannot change execution state: this service holds no mutating dependency, and any proposal that could
 * become action (a reassessment) must pass the deterministic {@link ReassessmentGovernor} through {@link ClosedLoopService} and a human decision.
 * No language model is invoked here; the text is produced deterministically so it is reproducible and cannot be prompt-injected.
 */
@Service
public class ExecutionAdvisoryService {
    public record Advisory(AgentRole role, String title, String text, List<String> evidence, boolean actionable) {}

    private final ExecutionSupport support;
    private final ExecutionAttemptRepositoryPort attempts;

    public ExecutionAdvisoryService(ExecutionSupport support, ExecutionAttemptRepositoryPort attempts) { this.support = support; this.attempts = attempts; }

    public Advisory explainFailure(String workflowId, String taskId) {
        ExecutionWorkflow w = support.load(workflowId, null);
        ExecutionTask t = w.task(taskId).orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
        List<ExecutionAttempt> history = attempts.attemptsByTask(taskId);
        List<String> evidence = new ArrayList<>();
        history.forEach(a -> evidence.add("attempt " + a.attemptNo() + " via " + a.adapterId() + " → " + a.outcome() + (a.message() != null ? ": " + a.message() : "")));
        String cause = switch (t.status()) {
            case FAILED -> "The task failed permanently: " + t.lastError() + ". It will not be retried automatically.";
            case RETRY_PENDING -> "A transient failure occurred (" + t.lastError() + "). A retry is allowed (" + t.attempts() + " of " + t.maxAttempts() + " attempts used).";
            case RECONCILIATION_REQUIRED -> "The external outcome is unknown (" + t.lastError() + "). Do not retry; confirm in the target system whether the action happened.";
            case BLOCKED -> "A predecessor failed or was blocked: " + t.lastError() + ". Downstream tasks cannot run until it is resolved.";
            default -> "The task is " + t.status() + "; no failure to explain.";
        };
        return new Advisory(roleFor(t.type()), "Why " + t.id() + " is " + t.status(), cause, evidence, false);
    }

    public Advisory summarizeDependencies(String workflowId) {
        ExecutionWorkflow w = support.load(workflowId, null);
        TaskGraph g = w.graph();
        String order = String.join(" → ", g.topologicalOrder());
        String blocked = w.tasks().stream().filter(t -> t.status() == TaskStatus.BLOCKED).map(ExecutionTask::id).collect(Collectors.joining(", "));
        List<String> evidence = w.tasks().stream().map(t -> t.id() + " " + t.status() + " depends on " + t.dependsOn()).toList();
        return new Advisory(AgentRole.COORDINATOR, "Dependency summary for " + w.id(),
                "Execution order: " + order + (blocked.isEmpty() ? "." : ". Blocked tasks: " + blocked + "."), evidence, false);
    }

    public Advisory explainDeviation(PlanOutcomeComparison cmp) {
        List<String> evidence = cmp.rows().stream().map(r -> r.metric() + ": expected " + r.expected() + ", actual " + (r.actual() == null ? "UNKNOWN" : r.actual())
                + " [" + r.status() + "]" + (r.deltaPercent() == null ? "" : String.format(", deviation %.1f%%", r.deltaPercent()))).toList();
        String text = cmp.maxMeasuredDeviationPercent().isPresent()
                ? String.format("The largest measured deviation is %.1f%%. Only real (non-simulated) measurements are counted.", cmp.maxMeasuredDeviationPercent().getAsDouble())
                : "No real measurements are available yet; deviation cannot be assessed (UNKNOWN or SIMULATED values are excluded).";
        return new Advisory(AgentRole.RECOVERY_PLANNING, "Plan versus actual", text, evidence, false);
    }

    private static AgentRole roleFor(TaskType t) {
        return switch (t.category()) {
            case PROCUREMENT, SUPPLIER_COMMUNICATION -> AgentRole.PROCUREMENT;
            case INVENTORY -> AgentRole.INVENTORY;
            case PRODUCTION -> AgentRole.PRODUCTION;
            case LOGISTICS -> AgentRole.LOGISTICS;
            case QUALITY -> AgentRole.QUALITY;
            default -> AgentRole.COORDINATOR;
        };
    }
}
