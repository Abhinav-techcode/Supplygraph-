package com.supplygraph.application.execution;

import com.supplygraph.domain.model.execution.ExecutionPolicy;
import com.supplygraph.domain.model.execution.ExecutionTask;
import com.supplygraph.domain.model.execution.TaskType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Chooses an adapter per task type. LIVE adapters are used only when the workspace policy explicitly allows live execution. */
@Component
public class AdapterSelector {
    private final List<ExecutionAdapter> adapters;

    public AdapterSelector(List<ExecutionAdapter> adapters) { this.adapters = List.copyOf(adapters); }

    public Optional<ExecutionAdapter> select(TaskType type, ExecutionPolicy policy) {
        Optional<ExecutionAdapter> live = policy.allowLiveExecution()
                ? adapters.stream().filter(a -> !a.simulated() && a.supportedTypes().contains(type)).findFirst() : Optional.empty();
        return live.isPresent() ? live : adapters.stream().filter(a -> a.simulated() && a.supportedTypes().contains(type)).findFirst();
    }

    /** A workflow is SIMULATED unless every task would be sent to a live adapter. */
    public boolean wouldBeSimulated(List<ExecutionTask> tasks, ExecutionPolicy policy) {
        return tasks.stream().anyMatch(t -> select(t.type(), policy).map(ExecutionAdapter::simulated).orElse(true));
    }

    public List<ExecutionAdapter> all() { return adapters; }
}
